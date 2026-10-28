package com.th4.edge.platform.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.platform.model.ServiceHealth;
import com.th4.edge.platform.model.SystemMetricPoint;
import com.th4.edge.platform.model.SystemStatus;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 无持久化依赖的服务器运行指标采集上下文。 */
@Note("系统监控上下文")
@Component
public class SystemMonitorContext {

    private static final int MAX_POINTS = 120;
    @Value("${decoboon.build-version:dev}")
    private String buildVersion;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Deque<SystemMetricPoint> history = new ArrayDeque<>();
    private final SystemInfo systemInfo = new SystemInfo();
    private long[] previousTicks;

    public SystemMonitorContext(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.previousTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    }

    /** 返回系统当前状态，并在内存中保留最近 120 次采样用于趋势展示。 */
    public synchronized SystemStatus status() {
        LocalDateTime now = LocalDateTime.now();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] ticks = processor.getSystemCpuLoadTicks();
        double cpu = safePercent(processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100D);
        previousTicks = ticks;

        GlobalMemory memory = systemInfo.getHardware().getMemory();
        double memoryUsage = safePercent((memory.getTotal() - memory.getAvailable()) * 100D / memory.getTotal());
        DiskMetric disk = diskMetric();
        long[] network = networkBytes();

        SystemMetricPoint point = new SystemMetricPoint();
        point.setSampleTime(now);
        point.setCpuUsage(round(cpu));
        point.setMemoryUsage(round(memoryUsage));
        point.setDiskUsage(round(disk.usage));
        point.setNetworkRecv(network[0]);
        point.setNetworkSent(network[1]);
        history.addLast(point);
        while (history.size() > MAX_POINTS) history.removeFirst();

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        SystemStatus status = new SystemStatus();
        status.setHostName(hostName());
        status.setOsName(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        status.setJavaVersion(System.getProperty("java.version"));
        status.setAppVersion(appVersion());
        status.setCpuCores(runtime.availableProcessors());
        status.setCpuUsage(point.getCpuUsage());
        status.setTotalMemory(memory.getTotal());
        status.setUsedMemory(usedMemory);
        status.setMaxMemory(runtime.maxMemory());
        status.setDiskTotal(disk.total);
        status.setDiskUsed(disk.used);
        status.setUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);
        status.setSystemLoad(round(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
        status.setHistory(new ArrayList<>(history));
        status.setServices(serviceHealth());
        status.setAlerts(alerts(status));
        return status;
    }

    private List<ServiceHealth> serviceHealth() {
        List<ServiceHealth> services = new ArrayList<>();
        long dbStart = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean healthy = connection.isValid(2);
            services.add(new ServiceHealth("MySQL", healthy ? "UP" : "DOWN", elapsedMs(dbStart),
                    healthy ? "连接池可用" : "连接有效性检查失败"));
        } catch (Exception exception) {
            services.add(new ServiceHealth("MySQL", "DOWN", elapsedMs(dbStart), rootMessage(exception)));
        }

        long redisStart = System.nanoTime();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            boolean healthy = "PONG".equalsIgnoreCase(pong);
            services.add(new ServiceHealth("Redis", healthy ? "UP" : "DOWN", elapsedMs(redisStart),
                    healthy ? "缓存服务可用" : "PING 返回 " + pong));
        } catch (Exception exception) {
            services.add(new ServiceHealth("Redis", "DOWN", elapsedMs(redisStart), rootMessage(exception)));
        }
        services.add(new ServiceHealth("应用进程", "UP", 0L, "JVM 运行正常"));
        return services;
    }

    private List<String> alerts(SystemStatus status) {
        List<String> alerts = new ArrayList<>();
        if (status.getCpuUsage() != null && status.getCpuUsage() >= 80D) alerts.add("CPU 使用率超过 80%");
        double memoryUsage = status.getTotalMemory() == null || status.getTotalMemory() == 0
                ? 0D : status.getUsedMemory() * 100D / status.getTotalMemory();
        if (memoryUsage >= 85D) alerts.add("系统内存使用率超过 85%");
        if (status.getDiskTotal() != null && status.getDiskTotal() > 0
                && status.getDiskUsed() * 100D / status.getDiskTotal() >= 85D) {
            alerts.add("磁盘使用率超过 85%");
        }
        status.getServices().stream()
                .filter(service -> !"UP".equals(service.getStatus()))
                .forEach(service -> alerts.add(service.getName() + " 服务不可用"));
        return alerts;
    }

    private DiskMetric diskMetric() {
        try {
            File file = new File(".");
            FileStore store = Files.getFileStore(Path.of(file.getCanonicalPath()));
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            return new DiskMetric(total, Math.max(0, total - free), total == 0 ? 0 : (total - free) * 100D / total);
        } catch (Exception exception) {
            return new DiskMetric(0L, 0L, 0D);
        }
    }

    private long[] networkBytes() {
        long recv = 0L;
        long sent = 0L;
        for (NetworkIF networkIF : systemInfo.getHardware().getNetworkIFs()) {
            networkIF.updateAttributes();
            recv += networkIF.getBytesRecv();
            sent += networkIF.getBytesSent();
        }
        return new long[]{recv, sent};
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private String appVersion() {
        Package applicationPackage = getClass().getPackage();
        String implementationVersion = applicationPackage == null ? null : applicationPackage.getImplementationVersion();
        return implementationVersion != null && !implementationVersion.isBlank() ? implementationVersion : buildVersion;
    }

    private double safePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) return 0D;
        return Math.min(value, 100D);
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private record DiskMetric(long total, long used, double usage) {}
}
