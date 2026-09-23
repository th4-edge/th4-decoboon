package com.th4.edge.decoboon.plan.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 计划模板来源。 */
@Note("计划模板来源")
public enum PlanTemplateSource implements Enumerable<String> {
    Baseline("平台基线"),
    Company("企业自定义");

    private final String name;

    PlanTemplateSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
