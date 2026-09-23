package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 经营动态分类。 */
@Note("经营动态分类")
public enum BusinessEventCategory implements Enumerable<String> {
    Customer("客户"),
    Contract("合同"),
    Project("项目"),
    Finance("资金");

    private final String name;

    BusinessEventCategory(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
