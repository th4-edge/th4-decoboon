package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 设计差异确认状态。 */
@Note("报价设计差异状态")
public enum QuoteDifferenceStatus implements Enumerable<String> {
    None("非混合报价"),
    Pending("待确认"),
    Confirmed("已确认"),
    Ignored("已忽略");

    private final String name;

    QuoteDifferenceStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
