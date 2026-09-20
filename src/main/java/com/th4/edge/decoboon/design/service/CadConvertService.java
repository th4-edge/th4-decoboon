package com.th4.edge.decoboon.design.service;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * CAD 图纸异步转换服务。
 * 将 DWG/DXF 转换为 PNG 预览图，回写到 Design.cadPreviewUrl。
 * 转换通过可配置的外部脚本执行，脚本不可用时标记为 failed，不影响主流程。
 *
 * 并发控制：核心线程 2、最大线程 4、队列 50，超出拒绝并标记 failed。
 * 超时控制：单个转换进程最长 120 秒，超时强制 kill。
 * 临时文件：每天凌晨 3 点清理超过 1 小时的临时目录。
 */
@Service
public class CadConvertService {

    private static final Logger log = LoggerFactory.getLogger(CadConvertService.class);

    /** 单个转换进程超时时间（秒） */
    private static final long PROCESS_TIMEOUT_SECONDS = 120;

    /** 临时目录保留时间（毫秒），超过则清理 */
    private static final long TEMP_DIR_TTL_MS = 60 * 60 * 1000L;

    private final ObjectStorage objectStorage;
    private final DesignRepository designRepository;
    private final ThreadPoolTaskExecutor executor;

    @Value("${decoboon.cad.convert-script:}")
    private String convertScript;

    @Value("${decoboon.cad.temp-dir:/tmp/cad-convert}")
    private String tempDir;

    public CadConvertService(ObjectStorage objectStorage, DesignRepository designRepository) {
        this.objectStorage = objectStorage;
        this.designRepository = designRepository;
        this.executor = buildExecutor();
    }

    private ThreadPoolTaskExecutor buildExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(4);
        pool.setQueueCapacity(50);
        pool.setThreadNamePrefix("cad-convert-");
        pool.setRejectedExecutionHandler((r, e) -> log.warn("CAD 转换线程池拒绝任务（队列已满）"));
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(30);
        pool.initialize();
        return pool;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(tempDir));
            log.info("CAD 转换服务初始化: 脚本={}, 临时目录={}, 线程池=核心2/最大4/队列50, 超时={}s",
                    convertScript, tempDir, PROCESS_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("CAD 临时目录创建失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }

    public void convertAsync(Long designId, String cadUrl) {
        if (cadUrl == null || cadUrl.isBlank()) {
            return;
        }
        if (isBusy()) {
            log.warn("CAD 转换繁忙（活跃={}/最大={}, 队列剩余={}），designId={} 标记为 failed，请稍后重试",
                    executor.getActiveCount(), executor.getMaxPoolSize(),
                    executor.getThreadPoolExecutor().getQueue().remainingCapacity(), designId);
            markFailed(designId);
            return;
        }
        executor.execute(() -> {
            try {
                convert(designId, cadUrl);
            } catch (Exception e) {
                log.error("CAD 转换失败 designId={}, cadUrl={}", designId, cadUrl, e);
                markFailed(designId);
            }
        });
    }

    private boolean isBusy() {
        return executor.getActiveCount() >= executor.getMaxPoolSize()
                && executor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0;
    }

    private void convert(Long designId, String cadUrl) throws Exception {
        updateStatus(designId, "processing", null);

        if (convertScript == null || convertScript.isBlank()) {
            log.warn("CAD 转换脚本未配置（decoboon.cad.convert-script），跳过转换");
            markFailed(designId);
            return;
        }
        File script = new File(convertScript);
        if (!script.exists() || !script.canExecute()) {
            log.warn("CAD 转换脚本不可执行: {}", convertScript);
            markFailed(designId);
            return;
        }

        Path workDir = Path.of(tempDir, UUID.randomUUID().toString());
        Files.createDirectories(workDir);

        try {
            String fileName = cadUrl.contains("/") ? cadUrl.substring(cadUrl.lastIndexOf('/') + 1) : "cad.dwg";
            File inputFile = workDir.resolve(fileName).toFile();
            try (InputStream in = objectStorage.get(cadUrl);
                 FileOutputStream out = new FileOutputStream(inputFile)) {
                if (in == null) {
                    throw new RuntimeException("CAD 文件不存在: " + cadUrl);
                }
                in.transferTo(out);
            }

            File outputPng = workDir.resolve("preview.png").toFile();
            ProcessBuilder pb = new ProcessBuilder(convertScript, inputFile.getAbsolutePath(), outputPng.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new RuntimeException("CAD 转换超时（>" + PROCESS_TIMEOUT_SECONDS + "秒），已强制终止");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("CAD 转换脚本退出码=" + exitCode + ", output=" + output);
            }
            if (!outputPng.exists() || outputPng.length() == 0) {
                throw new RuntimeException("CAD 转换输出文件为空");
            }

            String objectKey = "design/cad-preview/" + designId + "-" + System.currentTimeMillis() + ".png";
            try (InputStream pngIn = Files.newInputStream(outputPng.toPath())) {
                objectStorage.put(objectKey, pngIn, outputPng.length(), "image/png");
            }

            String previewUrl = "upload/" + objectKey;
            updateStatus(designId, "success", previewUrl);
            log.info("CAD 转换成功 designId={}, preview={}", designId, previewUrl);

        } finally {
            deleteRecursively(workDir);
        }
    }

    private void updateStatus(Long designId, String status, String previewUrl) {
        designRepository.findById(designId).ifPresent(design -> {
            design.setCadConvertStatus(status);
            if (previewUrl != null) {
                design.setCadPreviewUrl(previewUrl);
            }
            designRepository.save(design);
        });
    }

    private void markFailed(Long designId) {
        updateStatus(designId, "failed", null);
    }

    private void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {}
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupTempDirs() {
        Path base = Path.of(tempDir);
        if (!Files.exists(base)) return;
        long now = System.currentTimeMillis();
        int[] cleaned = {0};
        long[] freedBytes = {0};
        try (Stream<Path> children = Files.list(base)) {
            children.filter(Files::isDirectory).forEach(dir -> {
                try {
                    long lastModified = Files.getLastModifiedTime(dir).toMillis();
                    if (now - lastModified > TEMP_DIR_TTL_MS) {
                        long size = directorySize(dir);
                        deleteRecursively(dir);
                        cleaned[0]++;
                        freedBytes[0] += size;
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            log.warn("CAD 临时目录清理失败: {}", e.getMessage());
        }
        if (cleaned[0] > 0) {
            log.info("CAD 临时目录清理完成: 清理 {} 个目录, 释放 {} MB", cleaned[0], freedBytes[0] / 1024 / 1024);
        }
    }

    private long directorySize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (Exception e) { return 0; }
                    })
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }
}
