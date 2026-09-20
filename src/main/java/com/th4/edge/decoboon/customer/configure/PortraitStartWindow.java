package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-计划开工时间。 */
@Note("画像-计划开工时间")
public enum PortraitStartWindow implements Enumerable<String> {

    WithinOneMonth("1个月内"),
    OneToThreeMonths("1-3个月"),
    ThreeToSixMonths("3-6个月"),
    HalfYearPlus("半年以上"),
    Undecided("待定");

    private final String name;

    PortraitStartWindow(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
