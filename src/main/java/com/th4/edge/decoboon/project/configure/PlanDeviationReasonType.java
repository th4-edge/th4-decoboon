package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 计划偏差原因分类。 */
@Note("计划偏差原因分类")
public enum PlanDeviationReasonType implements Enumerable<String> {
    Customer("客户原因"),
    Internal("非客户原因");

    private final String name;

    PlanDeviationReasonType(String name) {
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
