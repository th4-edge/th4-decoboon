package com.th4.edge.components.storage.configure;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.th4.edge.components.storage.sdk.aliyun.AliyunObjectStorage;
import com.th4.edge.components.storage.sdk.local.LocalObjectStorage;
import com.th4.edge.components.storage.service.ObjectStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 对象存储厂商装配；业务层只依赖 ObjectStorage。 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class ObjectStorageConfigure {

    @Bean
    @ConditionalOnProperty(prefix = "th4.edge.components.storage", name = "provider", havingValue = "local", matchIfMissing = true)
    public ObjectStorage localObjectStorage(@Value("${th4.edge.framework.upload-dir:./data/upload/}") String uploadDir,
                                            StorageProperties properties) {
        return new LocalObjectStorage(uploadDir, properties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "th4.edge.components.storage", name = "provider", havingValue = "oss")
    public AliyunObjectStorage aliyunObjectStorage(StorageProperties properties) {
        require(properties.getEndpoint(), "ALIYUN_OSS_ENDPOINT");
        require(properties.getBucket(), "ALIYUN_OSS_BUCKET");
        require(properties.getAccessKeyId(), "ALIYUN_OSS_ACCESS_KEY_ID");
        require(properties.getAccessKeySecret(), "ALIYUN_OSS_ACCESS_KEY_SECRET");
        OSS client = new OSSClientBuilder().build(
                properties.getEndpoint(),
                new DefaultCredentialProvider(properties.getAccessKeyId(), properties.getAccessKeySecret()));
        return new AliyunObjectStorage(client, properties);
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 未配置，OSS 存储拒绝启动");
        }
    }
}
