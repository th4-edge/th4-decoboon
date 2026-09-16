package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 设计方案状态。 */
@Note("设计方案状态")
public enum DesignStatus implements Enumerable<String> {
    Draft("草稿"),
    Ready("待谈单"),
    Presented("已讲解"),
    Selected("客户选中"),
    Archived("已归档");

    private final String name;

    DesignStatus(String name) {
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
