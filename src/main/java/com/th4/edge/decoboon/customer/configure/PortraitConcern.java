package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-主要顾虑。 */
@Note("画像-主要顾虑")
public enum PortraitConcern implements Enumerable<String> {

    Price("价格"),
    Duration("工期"),
    Quality("质量"),
    Eco("环保"),
    AfterSales("售后");

    private final String name;

    PortraitConcern(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
