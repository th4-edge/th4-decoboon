package com.th4.edge.components.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明式业务并发锁；key 为 SpEL，可引用方法参数（如 #companyId / #stageId）。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLocked {

    String key();

    /**
     * 额外加锁的 SpEL 键，按声明顺序在 {@link #key()} 之后获取、逆序释放。
     *
     * <p>用于一个方法需要同时串行化两类互斥对象的场景（如业主邀请：既要按邀请串行，
     * 又要按「微信身份 + 项目」授权串行）。注意所有调用方必须保持相同的加锁顺序，避免死锁。</p>
     */
    String[] extraKeys() default {};

    long waitMillis() default 1000;

    long leaseMillis() default 30000;

    String message() default "业务正在处理中，请稍后重试";
}
