package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("工资项状态")
public enum PayrollItemStatus implements Enumerable<String> {

    Draft("草稿"),
    Confirmed("已确认"),
    Paid("已发放"),
    Void("已作废");

    private final String name;

    PayrollItemStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
