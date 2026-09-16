package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 应付状态。 */
@Note("应付状态")
public enum PayableStatus implements Enumerable<String> {

    Pending("待付款"),
    Partial("部分付款"),
    Paid("已付清"),
    Overdue("已逾期"),
    Closed("已关闭");

    private final String name;

    PayableStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
