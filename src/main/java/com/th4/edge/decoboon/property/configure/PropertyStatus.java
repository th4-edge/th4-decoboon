package com.th4.edge.decoboon.property.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 房屋装修业务状态。 */
@Note("房屋状态")
public enum PropertyStatus implements Enumerable<String> {
    PendingMeasure("待量房"),
    Measuring("量房中"),
    Measured("已量房"),
    Designing("设计中"),
    Quoted("已报价"),
    Contracted("已签约"),
    Serving("施工中"),
    Completed("已完工");

    private final String name;

    PropertyStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
