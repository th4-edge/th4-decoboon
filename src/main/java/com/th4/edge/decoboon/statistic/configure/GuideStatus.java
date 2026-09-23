package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 启动引导聚合状态。 */
@Note("启动引导状态")
public enum GuideStatus implements Enumerable<String> {
    Completed("已完成"),
    ActionRequired("待处理"),
    WaitingAssignment("等待分配"),
    Unknown("暂不可确认");

    private final String name;

    GuideStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
