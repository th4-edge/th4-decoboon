package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价生成路线。 */
@Note("报价模式")
public enum QuoteMode implements Enumerable<String> {
    Package("套餐报价"),
    Custom("自定义报价"),
    Hybrid("混合报价");

    private final String name;

    QuoteMode(String name) {
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
