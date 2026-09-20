package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-装修动机。 */
@Note("画像-装修动机")
public enum PortraitPurpose implements Enumerable<String> {

    FirstHome("自住首装"),
    Improve("改善"),
    Wedding("婚房"),
    SchoolDistrict("学区"),
    Investment("投资出租"),
    Partial("局部改造");

    private final String name;

    PortraitPurpose(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
