package com.th4.edge.components.storage.sdk.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.th4.edge.components.storage.configure.StorageProperties;
import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;

import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

/** 阿里云 OSS ObjectStorage 实现。对象默认私有，仅服务端授权后签发短时 URL。 */
public class AliyunObjectStorage implements ObjectStorage {

    private final OSS client;
    private final StorageProperties properties;

    public AliyunObjectStorage(OSS client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public StoredObject put(String key, InputStream input, long size, String contentType) {
        String objectKey = objectKey(key);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            if (contentType != null && !contentType.isBlank()) metadata.setContentType(contentType);
            var result = client.putObject(properties.getBucket(), objectKey, input, metadata);
            return new StoredObject(key, size, contentType, result.getETag());
        } catch (Exception ex) {
            throw storageError("OSS 对象上传失败", ex);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(properties.getBucket(), objectKey(key)).getObjectContent();
        } catch (Exception ex) {
            throw notFound();
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client.doesObjectExist(properties.getBucket(), objectKey(key));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(properties.getBucket(), objectKey(key));
        } catch (Exception ex) {
            throw storageError("OSS 对象删除失败", ex);
        }
    }

    @Override
    public String presignedGet(String key, Duration ttl) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + Math.max(30, ttl.toSeconds()) * 1000L);
            return client.generatePresignedUrl(properties.getBucket(), objectKey(key), expiration).toString();
        } catch (Exception ex) {
            throw storageError("OSS 临时地址签发失败", ex);
        }
    }

    @Override
    public boolean remote() {
        return true;
    }

    public void close() {
        client.shutdown();
    }

    private String objectKey(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.startsWith("/")) throw notFound();
        String prefix = properties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) return key;
        String normalized = prefix.replace('\\', '/').replaceAll("^/+|/+$", "");
        return normalized.isBlank() ? key : normalized + "/" + key;
    }

    private DefinedException notFound() {
        return new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
    }

    private DefinedException storageError(String message, Exception cause) {
        return new DefinedException(new DefaultErrorCode("500", message + "：" + cause.getMessage()));
    }
}
