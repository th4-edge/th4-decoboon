package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 收款状态。 */
@Note("收款状态")
public enum ReceiptStatus implements Enumerable<String> {
    Active("有效"),
    Reversed("已冲正");

    private final String name;

    ReceiptStatus(String name) {
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
