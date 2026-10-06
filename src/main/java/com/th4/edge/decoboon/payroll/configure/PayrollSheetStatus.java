package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("PayrollSheetStatus")
public enum PayrollSheetStatus implements Enumerable<String> {

    Draft("草稿"),
    Pending("待确认"),
    Confirmed("已确认"),
    PartialPaid("部分发放"),
    Paid("已发放"),
    Closed("已关闭"),
    Void("已作废"),
    ;

    private final String name;

    PayrollSheetStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
