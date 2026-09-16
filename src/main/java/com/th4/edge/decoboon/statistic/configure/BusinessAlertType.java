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
}
