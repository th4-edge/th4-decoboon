package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 计划偏差状态。 */
@Note("计划偏差状态")
public enum PlanDeviationStatus implements Enumerable<String> {
    Pending("待确认"),
    Confirmed("已确认"),
    Rejected("已驳回"),
    RoutedToChange("转合同工期变更"),
    Superseded("已被取代");

    private final String name;

    PlanDeviationStatus(String name) {
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
