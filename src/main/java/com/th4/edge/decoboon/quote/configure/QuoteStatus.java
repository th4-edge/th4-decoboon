package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 客户报价状态。
 */
@Note("报价状态")
public enum QuoteStatus implements Enumerable<String> {
    Draft("草稿"),
    Submitted("已提交"),
    Accepted("客户接受"),
    Rejected("客户拒绝"),
    Superseded("已被替代"),
    Converted("已转项目");

    private final String name;

    QuoteStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
