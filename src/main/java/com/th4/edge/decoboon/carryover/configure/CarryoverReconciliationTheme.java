package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 首月双轨对账主题。 */
@Note("对账主题")
public enum CarryoverReconciliationTheme implements Enumerable<String> {
    Receivable("项目未收"),
    Payable("项目未付"),
    Cash("资金余额"),
    Cost("项目成本");

    private final String name;

    CarryoverReconciliationTheme(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    @Override
    public String getValue() { return toString(); }
}
