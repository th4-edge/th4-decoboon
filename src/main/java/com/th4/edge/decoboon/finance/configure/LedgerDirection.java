package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 资金流水方向。 */
@Note("流水方向")
public enum LedgerDirection implements Enumerable<String> {

    Income("收入"),
    Expense("支出");

    private final String name;

    LedgerDirection(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
