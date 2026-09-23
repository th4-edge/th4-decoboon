package com.th4.edge.decoboon.document.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 单据记录状态。 */
@Note("单据状态")
public enum DocumentStatus implements Enumerable<String> {
    Active("有效"),
    Voided("已作废");

    private final String name;

    DocumentStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
