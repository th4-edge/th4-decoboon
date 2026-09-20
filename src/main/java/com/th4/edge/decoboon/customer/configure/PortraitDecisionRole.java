package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-决策角色。 */
@Note("画像-决策角色")
public enum PortraitDecisionRole implements Enumerable<String> {

    Self("本人决策"),
    Couple("夫妻共同"),
    Parents("父母出资"),
    Children("子女代订"),
    Unknown("待确认");

    private final String name;

    PortraitDecisionRole(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
