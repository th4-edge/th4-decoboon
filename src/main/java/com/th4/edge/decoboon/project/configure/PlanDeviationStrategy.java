package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 计划偏差顺延方式。 */
@Note("计划偏差顺延方式")
public enum PlanDeviationStrategy implements Enumerable<String> {
    Extend("整体顺延"),
    Compress("压缩后续阶段");

    private final String name;

    PlanDeviationStrategy(String name) {
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
