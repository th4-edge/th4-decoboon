package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/** 私有文件短期签名和本地文件解析。 */
@Component
public class FileSignatureContext {

    private static final String HMAC = "HmacSHA256";

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

    public String sign(String rawPath) {
        String path = normalize(rawPath);
        String remoteUrl = storage.presignedGet(path, java.time.Duration.ofSeconds(ttlSeconds));
        if (remoteUrl != null && !remoteUrl.isBlank()) {
            return remoteUrl;
        }
        long expires = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String signature = signature(path, expires);
        return "/file/private/download?path=" + encode(path)
                + "&expires=" + expires + "&signature=" + encode(signature);
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
