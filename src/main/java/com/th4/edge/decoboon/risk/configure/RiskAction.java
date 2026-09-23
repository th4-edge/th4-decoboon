package com.th4.edge.decoboon.risk.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 风险处置流水动作。 */
@Note("风险处置动作")
public enum RiskAction implements Enumerable<String> {

    Assign("指派"),
    Claim("认领"),
    Comment("处置记录"),
    Urge("催办"),
    Close("关闭"),
    Reopen("重开"),
    Void("自动消解");

    private final String name;

    RiskAction(String name) { this.name = name; }

    @Override
    public String getName() { return name; }
}
