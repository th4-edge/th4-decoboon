package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目事实追踪的展示视图；默认按业务阶段展示。 */
@Note("项目事实视图模式")
public enum ProjectFactViewMode implements Enumerable<String> {
    PHASE("阶段"),
    TIMELINE("时间线"),
    RELATION("关系链"),
    EXCEPTION("异常");

    private final String name;

    ProjectFactViewMode(String name) {
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
