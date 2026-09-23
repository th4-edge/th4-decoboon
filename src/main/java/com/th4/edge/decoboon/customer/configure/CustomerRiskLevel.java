package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业负责人客户风险等级。 */
@Note("客户风险等级")
public enum CustomerRiskLevel implements Enumerable<String> {
    Normal("正常"),
    Attention("需关注"),
    High("高风险");

    private final String name;

    CustomerRiskLevel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
