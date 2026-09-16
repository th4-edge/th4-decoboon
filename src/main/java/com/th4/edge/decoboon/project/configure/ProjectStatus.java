package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 项目生命周期状态。
 */
@Note("项目状态")
public enum ProjectStatus implements Enumerable<String> {
    Preparing("待开工"),
    Ongoing("施工中"),
    Suspended("已停工"),
    Accepting("待验收"),
    Finished("已完工"),
    Settled("已结算");

    private final String name;

    ProjectStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
