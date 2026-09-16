package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 经营预警级别。 */
@Note("经营预警级别")
public enum BusinessAlertLevel implements Enumerable<String> {
    Low("关注"),
    Medium("警告"),
    High("严重");

    private final String name;

    BusinessAlertLevel(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
