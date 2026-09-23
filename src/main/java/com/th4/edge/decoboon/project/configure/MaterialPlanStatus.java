package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 项目材料计划状态。
 *
 * <p>主链路：Planned（待选材/待确认）→ ReadyToOrder（可下单）→
 * PartiallyOrdered（部分下单）/ Ordered（已下单）→ PartiallyArrived（部分到场）/
 * Arrived（已到场）。Requested/Approved 为历史兼容状态，不再写入新数据。</p>
 */
@Note("材料计划状态")
public enum MaterialPlanStatus implements Enumerable<String> {
    Planned("待选材"),
    ReadyToOrder("可下单"),
    Requested("已请购"),
    Approved("已审批"),
    PartiallyOrdered("部分下单"),
    Ordered("已下单"),
    PartiallyArrived("部分到场"),
    Arrived("已到场"),
    Closed("已关闭"),
    Cancelled("已取消");

    private final String name;

    MaterialPlanStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
