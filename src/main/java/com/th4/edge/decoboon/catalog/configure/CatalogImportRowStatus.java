package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价项目库导入行状态（PRD §11.1）。 */
@Note("报价项目库导入行状态")
public enum CatalogImportRowStatus implements Enumerable<String> {

    Ready("待导入"),
    Duplicate("重复跳过"),
    SuspectedDuplicate("疑似重复"),
    Error("错误"),
    Imported("已导入"),
    Skipped("已跳过");

    private final String name;

    CatalogImportRowStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return toString();
    }
}
