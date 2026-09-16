package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户诉求分类。 */
@Note("客户诉求分类")
public enum RequirementCategory implements Enumerable<String> {
    Function("功能"),
    Style("风格"),
    Material("材料"),
    Budget("预算"),
    Schedule("工期"),
    Quality("质量"),
    Other("其他");

    private final String name;

    RequirementCategory(String name) {
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
