package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 工作项业务域。
 *
 * <p>用于让工作台卡片知道自己属于哪条业务条线，从而决定下钻目标与主行动文案。
 * 只做投影分类，不对应任何数据库枚举列。</p>
 */
@Note("工作项业务域")
public enum WorkItemDomain implements Enumerable<String> {
    Measure("量房"),
    Design("设计"),
    Quote("报价"),
    Contract("签约"),
    Project("项目"),
    Material("材料"),
    Acceptance("验收"),
    Change("变更"),
    Finance("资金");

    private final String name;

    WorkItemDomain(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
