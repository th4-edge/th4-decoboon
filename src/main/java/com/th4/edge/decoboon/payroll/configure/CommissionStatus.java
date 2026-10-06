package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("CommissionStatus")
public enum CommissionStatus implements Enumerable<String> {

    PendingAccrual("待计提"),
    Accrued("已计提"),
    Released("已释放"),
    InPayment("已入发放"),
    Paid("已发放"),
    Closed("已关闭"),
    Adjusted("已冲减"),
    ;

    private final String name;

    CommissionStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
