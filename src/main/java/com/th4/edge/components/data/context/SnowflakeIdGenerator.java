package com.th4.edge.components.data.context;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** 本地 Snowflake 风格 Long 主键生成器。 */
public final class SnowflakeIdGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private SnowflakeIdGenerator() {
    }

    public static long nextId() {
        long millis = System.currentTimeMillis();
        long floor = millis * 1_000_000L + ThreadLocalRandom.current().nextLong(1_000_000L);
        return SEQUENCE.updateAndGet(previous -> Math.max(previous + 1, floor));
    }
}
