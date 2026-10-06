package com.th4.edge.components.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁组件。
 */
@Component
@ConditionalOnProperty(prefix = "th4.edge.components.lock", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RedissonLock implements DistributedLock {

    private static final String LOCK_PREFIX = "th4:lock:";

    private final RedissonClient redissonClient;

    public RedissonLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 在等待时间内尝试获取锁，成功后使用固定租约时间，避免业务异常导致永久持有。
     */
    public boolean tryLock(String name, long waitMillis, long leaseMillis) {
        try {
            return lock(name).tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LockException("获取分布式锁被中断: " + name, exception);
        } catch (Exception exception) {
            throw new LockException("获取分布式锁失败: " + name, exception);
        }
    }

    public void unlock(String name) {
        RLock lock = lock(name);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private RLock lock(String name) {
        return redissonClient.getLock(LOCK_PREFIX + name);
    }
}
