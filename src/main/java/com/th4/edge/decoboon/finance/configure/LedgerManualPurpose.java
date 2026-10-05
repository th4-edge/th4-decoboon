package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 手工记账账务用途：用于约束手工记账边界与项目经营口径。 */
@Note("手工账务用途")
public enum LedgerManualPurpose implements Enumerable<String> {

    ProjectIncidentalCost("项目零星成本"),
    CompanyExpense("企业费用"),
    OtherIncome("其他收入"),
    CapitalTransfer("资金往来"),
    Other("其他");

    private final String name;

    LedgerManualPurpose(String name) {
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
