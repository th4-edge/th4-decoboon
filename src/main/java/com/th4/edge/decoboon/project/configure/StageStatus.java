package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 项目阶段状态。
 */
@Note("阶段状态")
public enum StageStatus implements Enumerable<String> {
    Todo("未开始"),
    Doing("施工中"),
    Accepting("待验收"),
    Done("已完成");

    private final String name;

    StageStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
