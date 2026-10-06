package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("CommissionAdjustmentType")
public enum CommissionAdjustmentType implements Enumerable<String> {

    Refund("退款冲减"),
    Resignation("离职调整"),
    CrossPeriod("跨期调整"),
    ;

    private final String name;

    CommissionAdjustmentType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
