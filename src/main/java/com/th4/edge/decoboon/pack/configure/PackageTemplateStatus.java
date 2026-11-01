package com.th4.edge.decoboon.pack.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 套餐模板生命周期状态。 */
@Note("套餐模板状态")
public enum PackageTemplateStatus implements Enumerable<String> {
    Draft("草稿"),
    Published("已发布"),
    Disabled("已停用");

    private final String name;

    PackageTemplateStatus(String name) {
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
