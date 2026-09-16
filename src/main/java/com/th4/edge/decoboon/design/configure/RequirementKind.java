package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户诉求类型。 */
@Note("客户诉求类型")
public enum RequirementKind implements Enumerable<String> {
    Demand("诉求"),
    Reject("排斥点");

    private final String name;

    RequirementKind(String name) {
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
