package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户诉求落地状态。 */
@Note("客户诉求状态")
public enum RequirementStatus implements Enumerable<String> {
    Pending("待确认"),
    Confirmed("已确认"),
    Resolved("已落地");

    private final String name;

    RequirementStatus(String name) {
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
