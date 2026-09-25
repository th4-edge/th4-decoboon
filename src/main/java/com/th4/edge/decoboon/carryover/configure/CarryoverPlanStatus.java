package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 接管后剩余施工计划状态。 */
@Note("接管后施工计划状态")
public enum CarryoverPlanStatus implements Enumerable<String> {
    NotStarted("待制定"),
    Draft("待重新确认"),
    Confirmed("已确认"),
    Locked("已锁定");

    private final String name;

    CarryoverPlanStatus(String name) {
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
