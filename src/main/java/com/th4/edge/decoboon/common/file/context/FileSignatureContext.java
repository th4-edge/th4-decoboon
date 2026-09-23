package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;

/** 私有文件短期签名、业务域名网关回源和本地文件解析。 */
@Component
public class FileSignatureContext {

    private static final String HMAC = "HmacSHA256";
    /** 对象存储回源落盘目录：业务域名网关下载依赖框架流式读取本地文件。 */
    private static final String SPOOL_DIR = ".download-spool";
    /** 回源临时文件保留时长，避免磁盘长期堆积，同时保证下载读取期间不被清理。 */
    private static final long SPOOL_TTL_MS = Duration.ofMinutes(10).toMillis();
    /** 网关下载展示文件名长度上限，避免异常长文件名影响下载头。 */
    private static final int DOWNLOAD_NAME_LIMIT = 120;

    private final Path uploadDir;
    private final ObjectStorage storage;
    private final SecretKeySpec key;
    private final long ttlSeconds;

    public FileSignatureContext(ObjectStorage storage,
                                @Value("${th4.edge.framework.upload-dir:./data/upload/}") String uploadDir,
                                @Value("${th4.edge.framework.file-signature-secret:}") String secret,
                                @Value("${th4.edge.framework.file-signature-ttl-seconds:300}") long ttlSeconds) {
        this.storage = storage;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.key = new SecretKeySpec(resolveSecret(secret), HMAC);
        this.ttlSeconds = Math.max(30, Math.min(ttlSeconds, 3600));
    }

    /**
     * 客户端直读地址：对象存储可用时返回第三方预签名地址，否则退回业务域名网关。
     * 供图片和附件预览使用；微信小程序下载必须使用 {@link #signGateway(String)}。
     */
    public String sign(String rawPath) {
        String path = normalize(rawPath);
        String remoteUrl = storage.presignedGet(path, Duration.ofSeconds(ttlSeconds));
        if (remoteUrl != null && !remoteUrl.isBlank()) {
            return remoteUrl;
        }
        return gateway(path, null);
    }

    public String signGateway(String rawPath) {
        return gateway(normalize(rawPath), null);
    }

    /**
     * 业务域名网关地址：微信小程序 downloadFile 只能访问已备案的业务域名，
     * 因此业务单据下载固定返回网关相对地址，由后端校验签名后回源对象存储。
     * 文件名仅作为下载展示名，签名仍只覆盖对象路径，避免历史链接失效。
     */
    public String signGateway(String rawPath, String fileName) {
        return gateway(normalize(rawPath), safeDownloadName(fileName));
    }

    public LocalDateTime expiresAt() {
        return LocalDateTime.ofInstant(Instant.now().plusSeconds(ttlSeconds), ZoneId.systemDefault());
    }

    public boolean verify(String rawPath, Long expires, String rawSignature) {
        if (expires == null || rawSignature == null || rawSignature.isBlank()) {
            return false;
        }
        if (expires < Instant.now().getEpochSecond()) {
            return false;
        }
        try {
            String path = normalize(rawPath);
            String expected = signature(path, expires);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    decode(rawSignature).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    public File materialize(String rawPath) {
        return materialize(rawPath, null);
    }

    /**
     * 校验通过后取回可下载文件：本地存储直接定位，对象存储回源到本地临时目录，
     * 由框架以流式响应返回；提供展示文件名时按该名称落盘，保证下载后文件名可读。
     */
    public File materialize(String rawPath, String fileName) {
        String path = normalize(rawPath);
        String safeName = safeDownloadName(fileName);
        if (safeName != null) {
            return spool(path, safeName);
        }
        if (storage != null && storage.remote()) {
            return spool(path, null);
        }
        return resolve(path).toFile();
    }

    public Path resolve(String rawPath) {
        String path = normalize(rawPath);
        Path target = uploadDir.resolve(path).normalize();
        try {
            Path root = uploadDir.toRealPath();
            Path real = target.toRealPath();
            if (!real.startsWith(root) || !Files.isRegularFile(real)) {
                throw notFound();
            }
            return real;
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw notFound();
        }
    }

    /** 将历史 /upload/...、完整 URL 或相对路径统一为 upload-dir 下的相对路径。 */
    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw notFound();
        }
        String value = raw.trim();
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                value = URI.create(value).getPath();
            } else if (value.startsWith("file:")) {
                value = URI.create(value).getPath();
            }
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw notFound();
        }
        value = value.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.startsWith("upload/")) {
            value = value.substring("upload/".length());
        }
        if (value.isBlank() || value.contains("..") || value.startsWith(".")) {
            throw notFound();
        }
        return value;
    }

    /** 判断路径是否属于受管私有对象，而不是任意外部 URL。 */
    public boolean managed(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) return false;
        value = value.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        return value.startsWith("upload/") || value.startsWith("companies/") || value.startsWith("system/");
    }

    private String gateway(String path, String fileName) {
        long expires = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String signature = signature(path, expires);
        String url = "/file/private/download?path=" + encode(path)
                + "&expires=" + expires + "&signature=" + encode(signature);
        return fileName == null ? url : url + "&name=" + encode(fileName);
    }

    private File spool(String path, String fileName) {
        try {
            Path dir = uploadDir.resolve(SPOOL_DIR);
            Files.createDirectories(dir);
            sweep(dir);
            Path bucket = dir.resolve(HexFormat.of().formatHex(sha256(path)));
            Files.createDirectories(bucket);
            Path target = bucket.resolve(spoolName(path, fileName));
            if (Files.isRegularFile(target)) {
                // 命中缓存时刷新时间，使清理按“最后使用时间”而不是首次生成时间
                Files.setLastModifiedTime(target, FileTime.fromMillis(System.currentTimeMillis()));
                return target.toFile();
            }
            Path temp = Files.createTempFile(bucket, "part-", ".tmp");
            try (InputStream input = storage.get(path)) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toFile();
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw notFound();
        }
    }

    private void sweep(Path dir) {
        long deadline = System.currentTimeMillis() - SPOOL_TTL_MS;
        try (Stream<Path> entries = Files.list(dir)) {
            entries.forEach(entry -> {
                try {
                    if (Files.getLastModifiedTime(entry).toMillis() >= deadline) return;
                    if (Files.isDirectory(entry)) {
                        deleteRecursively(entry);
                    } else {
                        Files.deleteIfExists(entry);
                    }
                } catch (Exception ignored) {
                    // 单个回源文件清理失败不影响本次下载
                }
            });
        } catch (Exception ignored) {
            // 目录列举失败不影响本次下载
        }
    }

    private void deleteRecursively(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (Exception ignored) {
                    // 忽略单个文件删除失败
                }
            });
        } catch (Exception ignored) {
            // 忽略目录删除失败
        }
    }

    private String spoolName(String path, String fileName) {
        String extension = extensionOf(path);
        if (fileName != null && !fileName.isBlank()) {
            return fileName.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT))
                    ? fileName : fileName + extension;
        }
        return HexFormat.of().formatHex(sha256(path)) + extension;
    }

    private String extensionOf(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot <= 0) return "";
        return name.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
    }

    /** 下载展示名去掉路径分隔符和控制字符，避免下载头被污染或写到目录之外。 */
    private String safeDownloadName(String name) {
        if (name == null || name.isBlank()) return null;
        String safe = name.trim()
                .replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "")
                .replaceAll("\\s+", "");
        if (safe.isBlank()) return null;
        return safe.length() > DOWNLOAD_NAME_LIMIT ? safe.substring(0, DOWNLOAD_NAME_LIMIT) : safe;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "文件摘要计算失败"));
        }
    }

    private String signature(String path, long expires) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(key);
            byte[] digest = mac.doFinal((path + "\n" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "文件签名失败"));
        }
    }

    private byte[] resolveSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return random;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private DefinedException notFound() {
        return new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
    }
}
