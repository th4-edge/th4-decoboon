package com.th4.edge.components.storage.service;

/** 对象存储写入结果。 */
public record StoredObject(String key, long size, String contentType, String etag) {
}
