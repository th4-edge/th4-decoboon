package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 施工任务状态。
 */
@Note("施工任务状态")
public enum TaskStatus implements Enumerable<String> {
    Todo("未开始"),
    Doing("施工中"),
    Accepting("待验收"),
    Done("已完成"),
    Cancelled("已取消");

    private final String name;

    TaskStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
