package com.th4.edge.components.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 测试/显式关闭分布式锁时的 no-op 实现；生产默认启用 RedissonLock。 */
@Component
@ConditionalOnProperty(prefix = "th4.edge.components.lock", name = "enabled", havingValue = "false")
public class DisabledDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DisabledDistributedLock.class);

    @Override
    public boolean tryLock(String name, long waitMillis, long leaseMillis) {
        log.warn("分布式锁已禁用（th4.edge.components.lock.enabled=false），直接执行业务: {}", name);
        return true;
    }

    @Override
    public void unlock(String name) {
        // no-op
    }
}
