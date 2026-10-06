package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("CommissionAdjustmentStatus")
public enum CommissionAdjustmentStatus implements Enumerable<String> {

    Pending("待下期抵扣"),
    Included("已进入工资单"),
    Cancelled("已取消"),
    ;

    private final String name;

    CommissionAdjustmentStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
