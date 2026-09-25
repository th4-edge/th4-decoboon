package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 接管前成本完整性。 */
@Note("接管前成本完整性")
public enum CarryoverCostStatus implements Enumerable<String> {
    Complete("已确认完整"),
    TotalOnly("只知道总额"),
    Unknown("暂不确认");

    private final String name;

    CarryoverCostStatus(String name) {
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

    public boolean complete() {
        return this == Complete;
    }
}
