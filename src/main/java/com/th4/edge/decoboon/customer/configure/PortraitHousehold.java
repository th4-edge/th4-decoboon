package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-居住人口结构。 */
@Note("画像-居住人口结构")
public enum PortraitHousehold implements Enumerable<String> {

    Single("独居"),
    Couple("两口"),
    ThreePlus("三口及以上"),
    ThreeGenerations("三代同堂"),
    Unknown("待确认");

    private final String name;

    PortraitHousehold(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
