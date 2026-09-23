package com.th4.edge.decoboon.document.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 单据输出对象。 */
@Note("单据输出对象")
public enum DocumentAudience implements Enumerable<String> {
    Customer("客户版"),
    Internal("内部版");

    private final String name;

    DocumentAudience(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
