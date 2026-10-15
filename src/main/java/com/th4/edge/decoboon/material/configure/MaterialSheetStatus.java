package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 主材下单作业单状态（单据/编排状态）。
 *
 * <p>与 {@code MaterialPlanStatus}（需求/履约状态）单向映射，禁止双写状态源：
 * 作业单只派生事实层动作，材料计划状态由采购与到货事实推进。</p>
 */
@Note("主材下单作业单状态")
public enum MaterialSheetStatus implements Enumerable<String> {

    Draft("填单中"),
    Measuring("待测量"),
    Measured("已测量"),
    Submitted("已提交请购"),
    PartiallyOrdered("部分下单"),
    Pricing("财务核价"),
    Ordered("已下单"),
    PartiallyArrived("部分到场"),
    Arrived("已到场"),
    Settled("已结算"),
    Closed("已关闭"),
    Cancelled("已取消");

    private final String name;

    MaterialSheetStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 是否已进入采购（进入后锁定选材，换材走增减项，DEC-008 / DEC-056）。 */
    public boolean ordered() {
        return this == Ordered || this == PartiallyOrdered || this == PartiallyArrived || this == Arrived
                || this == Settled || this == Closed;
    }

    /** 是否允许编辑单据内容。 */
    public boolean editable() {
        return this == Draft || this == Measuring || this == Measured || this == Submitted || this == Pricing;
    }
}
