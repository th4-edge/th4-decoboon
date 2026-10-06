package com.th4.edge.components.lock;

/** 业务并发分布式锁契约；生产实现为 RedissonLock。 */
public interface DistributedLock {

    boolean tryLock(String name, long waitMillis, long leaseMillis);

    void unlock(String name);
}
