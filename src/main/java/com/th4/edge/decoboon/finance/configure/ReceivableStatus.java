package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 应收计划状态。 */
@Note("应收状态")
public enum ReceivableStatus implements Enumerable<String> {

    Pending("待收"),
    Partial("部分收款"),
    Received("已收清"),
    Overdue("已逾期"),
    Closed("已关闭");

    private final String name;

    ReceivableStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
