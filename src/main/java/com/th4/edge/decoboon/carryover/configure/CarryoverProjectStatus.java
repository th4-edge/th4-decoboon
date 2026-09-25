package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 单项目承接状态。 */
@Note("承接项目状态")
public enum CarryoverProjectStatus implements Enumerable<String> {
    Draft("待完善"),
    Ready("待确认"),
    Confirmed("确认通过"),
    Applied("已承接"),
    Locked("已续营"),
    Cancelled("已取消");

    private final String name;

    CarryoverProjectStatus(String name) {
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

    public boolean editable() {
        return this == Draft || this == Ready;
    }
}
