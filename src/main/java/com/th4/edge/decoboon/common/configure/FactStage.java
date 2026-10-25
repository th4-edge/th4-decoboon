package com.th4.edge.decoboon.common.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目事实追踪的一级阶段，顺序为正式业务口径。 */
@Note("业务事实阶段")
public enum FactStage implements Enumerable<String> {
    Source("来源", 10),
    PreSale("售前", 20),
    Signing("签约", 30),
    Fulfillment("履约", 40),
    Change("变更", 50),
    Acceptance("验收", 60),
    Finance("财务", 70),
    AfterSales("售后", 80);

    private final String name;
    private final int order;

    FactStage(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
