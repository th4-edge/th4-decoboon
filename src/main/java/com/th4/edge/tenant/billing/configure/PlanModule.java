package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 套餐包含的业务功能模块。
 */
@Note("套餐功能模块")
public enum PlanModule implements Attribute<String> {

    Crm("客户经营"),
    Design("设计转化"),
    Quote("报价合同"),
    Project("项目履约"),
    Supply("供应链与成本"),
    Finance("财务结算"),
    Analytics("经营分析"),
    Ai("AI辅助"),
    Api("开放接口");

    private final String name;

    PlanModule(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getName() {
        return name;
    }
}
