package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 在跑项目承接任务状态。 */
@Note("承接任务状态")
public enum CarryoverBatchStatus implements Enumerable<String> {
    Active("进行中"),
    Completed("已完成"),
    Cancelled("已取消");

    private final String name;

    CarryoverBatchStatus(String name) {
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
