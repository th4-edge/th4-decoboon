package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 剩余收付款计划方向：期初应收 / 期初应付。 */
@Note("剩余收付款计划方向")
public enum CarryoverPaymentDirection implements Enumerable<String> {
    Receivable("期初应收"),
    Payable("期初应付");

    private final String name;

    CarryoverPaymentDirection(String name) {
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
