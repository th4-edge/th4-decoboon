package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("PayslipStatus")
public enum PayslipStatus implements Enumerable<String> {

    PendingPayment("待发放"),
    Published("已发布"),
    Void("已作废"),
    ;

    private final String name;

    PayslipStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
