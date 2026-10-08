package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户休眠原因。 */
@Note("客户休眠原因")
public enum CustomerDormantReason implements Enumerable<String> {
    PlanPostponed("装修计划延期"),
    BudgetUndecided("预算或方案未定"),
    DecisionPending("决策未确定"),
    CustomerRequestedPause("客户要求暂缓联系"),
    ContactUnreachable("长期无法有效联系"),
    LeadPendingVerification("线索待核实"),
    Other("其他");

    private final String name;

    CustomerDormantReason(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
