package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 接管后剩余施工计划动作。 */
@Note("接管后计划动作")
public enum CarryoverPlanAction implements Enumerable<String> {
    StageCreate("新增阶段"),
    StageUpdate("修改阶段"),
    StageReorder("调整阶段顺序"),
    StageDelete("删除阶段"),
    Confirm("确认计划");

    private final String name;

    CarryoverPlanAction(String name) {
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
