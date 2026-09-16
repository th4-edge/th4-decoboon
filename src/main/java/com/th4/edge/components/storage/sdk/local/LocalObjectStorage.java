package com.th4.edge.components.storage.sdk.local;

import com.th4.edge.components.storage.configure.StorageProperties;
import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** 开发与测试环境的本地 ObjectStorage 实现，生产必须切换 Oss。 */
public class LocalObjectStorage implements ObjectStorage {

    private final Path root;
    private final StorageProperties properties;

    public LocalObjectStorage(String uploadDir, StorageProperties properties) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.properties = properties;
    }

    @Override
    public StoredObject put(String key, InputStream input, long size, String contentType) {
        Path target = resolveNew(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject(key, size, contentType, null);
        } catch (Exception ex) {
            throw storageError("本地文件写入失败", ex);
        }
    }

    @Override
    public InputStream get(String key) {
        Path target = resolveExisting(key);
        try {
            return Files.newInputStream(target);
        } catch (Exception ex) {
            throw notFound();
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Files.isRegularFile(resolveExisting(key));
        } catch (DefinedException ex) {
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveExisting(key));
        } catch (Exception ex) {
            throw storageError("本地文件删除失败", ex);
        }
    }

    @Override
    public String presignedGet(String key, Duration ttl) {
        return null;
    }

    @Override
    public boolean remote() {
        return false;
    }

    public Path resolve(String key) {
        return resolveExisting(key);
    }

    private Path resolveNew(String key) {
        String normalized = validatedKey(key);
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root)) throw notFound();
        return target;
    }

    private Path resolveExisting(String key) {
        Path target = resolveNew(key);
        if (!Files.isRegularFile(target)) throw notFound();
        return target;
    }

    private String validatedKey(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.startsWith("/")) throw notFound();
        String prefix = properties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) return key;
        String normalizedPrefix = prefix.replace('\\', '/').replaceAll("^/+|/+$", "");
        return normalizedPrefix.isBlank() ? key : normalizedPrefix + "/" + key;
    }

    private DefinedException notFound() {
        return new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
    }

    private DefinedException storageError(String message, Exception cause) {
        return new DefinedException(new DefaultErrorCode("500", message + "：" + cause.getMessage()));
    }
}
