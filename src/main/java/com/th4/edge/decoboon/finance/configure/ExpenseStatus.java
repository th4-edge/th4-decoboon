package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 费用/报销状态。 */
@Note("费用报销状态")
public enum ExpenseStatus implements Enumerable<String> {

    Draft("草稿"),
    Submitted("已提交"),
    Returned("退回补充"),
    Approved("已批准"),
    Rejected("已拒绝"),
    Cancelled("已取消"),
    Voided("已作废"),
    Closed("已关闭");

    private final String name;

    ExpenseStatus(String name) {
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
