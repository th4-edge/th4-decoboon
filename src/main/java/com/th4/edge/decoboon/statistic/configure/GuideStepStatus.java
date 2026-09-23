package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 启动引导步骤状态。 */
@Note("启动引导步骤状态")
public enum GuideStepStatus implements Enumerable<String> {
    Done("已完成"),
    Todo("待完成"),
    Waiting("等待分配"),
    Unknown("暂不可确认");

    private final String name;

    GuideStepStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
