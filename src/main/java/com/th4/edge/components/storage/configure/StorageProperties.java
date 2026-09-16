package com.th4.edge.components.storage.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 通用对象存储配置。生产环境只允许通过环境变量注入密钥。 */
@ConfigurationProperties(prefix = "th4.edge.components.storage")
public class StorageProperties {

    private String provider = "local";
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private long presignTtlSeconds = 300;
    private long maxFileSizeBytes = 524_288_000L;
    private String keyPrefix = "";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    public long getPresignTtlSeconds() { return presignTtlSeconds; }
    public void setPresignTtlSeconds(long presignTtlSeconds) { this.presignTtlSeconds = presignTtlSeconds; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
}
