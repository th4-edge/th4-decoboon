package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 首月双轨对账状态。 */
@Note("对账状态")
public enum CarryoverReconciliationStatus implements Enumerable<String> {
    Draft("草稿"),
    Pending("待对账"),
    DiffHandling("差异处理中"),
    Confirmed("已确认"),
    Closed("已关闭");

    private final String name;

    CarryoverReconciliationStatus(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    @Override
    public String getValue() { return toString(); }
}
