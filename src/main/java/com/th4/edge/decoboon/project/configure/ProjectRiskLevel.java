package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目经营风险等级。 */
@Note("项目风险等级")
public enum ProjectRiskLevel implements Enumerable<String> {
    Normal("正常"),
    Attention("需关注"),
    High("高风险");

    private final String name;

    ProjectRiskLevel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
