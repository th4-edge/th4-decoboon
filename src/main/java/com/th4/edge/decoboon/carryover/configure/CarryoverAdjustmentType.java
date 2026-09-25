package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 接管锁定后的调整类型。 */
@Note("承接调整类型")
public enum CarryoverAdjustmentType implements Enumerable<String> {
    Receivable("期初应收调整"),
    Payable("期初应付调整"),
    Cost("接管前成本调整"),
    Other("其他更正");

    private final String name;

    CarryoverAdjustmentType(String name) {
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
