package com.th4.edge.decoboon.plan.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业计划模板状态。 */
@Note("企业计划模板状态")
public enum PlanTemplateStatus implements Enumerable<String> {
    Draft("草稿"),
    Enabled("已启用"),
    Disabled("已停用");

    private final String name;

    PlanTemplateStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
