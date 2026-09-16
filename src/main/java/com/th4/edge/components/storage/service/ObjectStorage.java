package com.th4.edge.components.storage.service;

import java.io.InputStream;
import java.time.Duration;

/** 与厂商无关的私有对象存储能力。 */
public interface ObjectStorage {

    StoredObject put(String key, InputStream input, long size, String contentType);

    InputStream get(String key);

    boolean exists(String key);

    void delete(String key);

    /** 返回临时 GET 地址；本地存储返回 null，由业务签名下载兜底。 */
    String presignedGet(String key, Duration ttl);

    boolean remote();
}
