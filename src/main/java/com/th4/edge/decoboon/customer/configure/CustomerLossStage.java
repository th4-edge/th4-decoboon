package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户流失时所在的签约前经营阶段。 */
@Note("客户流失阶段")
public enum CustomerLossStage implements Enumerable<String> {
    FollowUp("待我跟进"),
    Measure("待我量房"),
    Design("待我设计"),
    Negotiation("待我谈单"),
    Signing("待我签约");

    private final String name;

    CustomerLossStage(String name) {
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
