package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目成本与毛利的展示阶段。 */
@Note("项目成本阶段")
public enum ProjectCostStage implements Enumerable<String> {
    Estimated("预计"),
    Dynamic("动态"),
    Final("最终");

    private final String name;

    ProjectCostStage(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
