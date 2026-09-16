package com.th4.edge.decoboon.property.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 量房记录状态。 */
@Note("量房状态")
public enum MeasureStatus implements Enumerable<String> {
    Scheduled("已预约"),
    Completed("已完成"),
    Cancelled("已取消");

    private final String name;

    MeasureStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
