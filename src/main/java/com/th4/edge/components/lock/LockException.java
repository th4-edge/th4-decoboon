package com.th4.edge.components.lock;

/**
 * 分布式锁操作异常。
 */
public class LockException extends RuntimeException {

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
