package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-房屋现状。 */
@Note("画像-房屋现状")
public enum PortraitHouseCondition implements Enumerable<String> {

    Rough("毛坯"),
    Old("旧房翻新"),
    Refined("精装改造"),
    Pending("待交付");

    private final String name;

    PortraitHouseCondition(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
