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

    long waitMillis() default 1000;

    long leaseMillis() default 30000;

    String message() default "业务正在处理中，请稍后重试";
}
