package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 经营动态展示级别。 */
@Note("经营动态级别")
public enum BusinessEventLevel implements Enumerable<String> {
    Normal("普通"),
    Attention("关注"),
    Milestone("里程碑"),
    Warning("警告"),
    Danger("异常");

    private final String name;

    BusinessEventLevel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
