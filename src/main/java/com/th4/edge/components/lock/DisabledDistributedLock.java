package com.th4.edge.components.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 显式关闭分布式锁时的单 JVM 本地锁实现。
 *
 * <p>仅用于测试或单进程环境；生产必须启用 RedissonLock 承担跨实例并发。</p>
 */
@Component
@ConditionalOnProperty(prefix = "th4.edge.components.lock", name = "enabled", havingValue = "false")
public class DisabledDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DisabledDistributedLock.class);

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String name, long waitMillis, long leaseMillis) {
        log.warn("分布式锁已禁用，使用单 JVM 本地锁: {}", name);
        ReentrantLock lock = locks.computeIfAbsent(name, key -> new ReentrantLock());
        try {
            return lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String name) {
        ReentrantLock lock = locks.get(name);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
