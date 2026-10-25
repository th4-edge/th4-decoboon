package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 签约前客户流失原因。 */
@Note("客户流失原因")
public enum CustomerLossReason implements Enumerable<String> {
    ChoseOtherCompany("选择了其他装修公司"),
    PriceBudgetMismatch("价格或预算不匹配"),
    DesignUnsatisfied("设计方案不满意"),
    ScheduleMismatch("工期或开工时间不匹配"),
    SelfOrRelative("自装或亲友装修"),
    ProjectCancelled("装修计划取消"),
    ServiceTrust("服务或信任问题"),
    Other("其他");

    private final String name;

    CustomerLossReason(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
