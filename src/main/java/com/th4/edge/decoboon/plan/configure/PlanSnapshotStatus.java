package com.th4.edge.decoboon.plan.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 合同计划快照状态。 */
@Note("合同计划状态")
public enum PlanSnapshotStatus implements Enumerable<String> {
    Draft("草稿"),
    Confirmed("已确认"),
    Superseded("已替代");

    private final String name;
    PlanSnapshotStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
