package com.th4.edge.components.lock.configure;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 分布式锁基础配置。
 *
 * <p>当框架或其他模块已经提供 RedissonClient 时自动退让，复用已有连接。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "th4.edge.components.lock", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RedissonLockConfigure {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisProperties properties) {
        Duration timeout = properties.getTimeout() == null ? DEFAULT_TIMEOUT : properties.getTimeout();
        Duration connectTimeout = properties.getConnectTimeout() == null
                ? timeout
                : properties.getConnectTimeout();

        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setDatabase(properties.getDatabase())
                .setTimeout(Math.toIntExact(timeout.toMillis()))
                .setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()))
                .setRetryAttempts(1);
        if (StringUtils.hasText(properties.getUsername())) {
            server.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            server.setPassword(properties.getPassword());
        }
        return Redisson.create(config);
    }
}
