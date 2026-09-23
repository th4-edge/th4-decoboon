package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 经营预警类型。 */
@Note("经营预警类型")
public enum BusinessAlertType implements Enumerable<String> {
    OverdueReceivable("逾期应收"),
    ProjectSuspensionRisk("停工风险"),
    MaterialOverIssue("材料超领"),
    AbnormalChangeRate("变更率异常");

    private final String name;

    BusinessAlertType(String name) { this.name = name; }
    @Override public String getName() { return name; }

    /**
     * 预警对应的负责人下一步动作。
     *
     * <p>首页风险项目与项目经营台账共用同一文案来源，避免同一预警出现两套处置建议。</p>
     */
    public String nextAction() {
        return switch (this) {
            case OverdueReceivable -> "跟踪应收回款，确认客户付款计划";
            case ProjectSuspensionRisk -> "核实停工原因，明确复工责任和日期";
            case MaterialOverIssue -> "核对材料计划和领料，处理超领差异";
            case AbnormalChangeRate -> "复核合同变更依据、成本影响和审批";
        };
    }
}
