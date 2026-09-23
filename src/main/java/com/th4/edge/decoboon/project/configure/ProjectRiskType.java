package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 项目经营风险类型。
 *
 * <p>与经营预警 {@code BusinessAlertType} 一一对应，用于把预警口径投影到项目台账，
 * 不新增第二套风险规则。</p>
 */
@Note("项目风险类型")
public enum ProjectRiskType implements Enumerable<String> {
    None("正常"),
    OverdueReceivable("回款逾期"),
    DeliveryRisk("交付风险"),
    MaterialOverIssue("材料超领"),
    ChangeAnomaly("变更异常");

    private final String name;

    ProjectRiskType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
