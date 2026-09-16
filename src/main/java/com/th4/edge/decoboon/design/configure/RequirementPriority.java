package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户诉求优先级。 */
@Note("客户诉求优先级")
public enum RequirementPriority implements Enumerable<String> {
    High("高"),
    Medium("中"),
    Low("低");

    private final String name;

    RequirementPriority(String name) {
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
