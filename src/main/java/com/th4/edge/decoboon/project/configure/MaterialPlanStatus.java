package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 项目材料计划状态。
 */
@Note("材料计划状态")
public enum MaterialPlanStatus implements Enumerable<String> {
    Planned("已计划"),
    Requested("已请购"),
    Approved("已审批"),
    Ordered("已下单"),
    PartiallyArrived("部分到场"),
    Arrived("已到场"),
    Closed("已关闭");

    private final String name;

    MaterialPlanStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
