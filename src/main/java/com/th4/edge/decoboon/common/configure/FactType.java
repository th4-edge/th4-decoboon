package com.th4.edge.decoboon.common.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 统一事实节点的默认类型与阶段内固定顺序。
 *
 * <p>只定义事实类型，不代表对应业务能力已经实现或已通过验收。</p>
 */
@Note("业务事实类型")
public enum FactType implements Enumerable<String> {
    Customer(FactStage.Source, 10, "客户"),
    Property(FactStage.Source, 20, "房屋"),
    ProjectOrigin(FactStage.Source, 30, "项目来源"),

    Measure(FactStage.PreSale, 10, "量房"),
    Design(FactStage.PreSale, 20, "设计方案"),
    Quote(FactStage.PreSale, 30, "报价"),

    QuoteAcceptance(FactStage.Signing, 10, "报价接受"),
    Contract(FactStage.Signing, 20, "合同"),

    Project(FactStage.Fulfillment, 10, "项目"),
    ConstructionPlan(FactStage.Fulfillment, 20, "施工计划"),
    Stage(FactStage.Fulfillment, 30, "施工阶段"),
    Task(FactStage.Fulfillment, 40, "施工任务"),
    SiteReport(FactStage.Fulfillment, 50, "现场日报"),
    MaterialSelection(FactStage.Fulfillment, 60, "主材选材"),
    MaterialPlan(FactStage.Fulfillment, 70, "材料计划"),
    Purchase(FactStage.Fulfillment, 80, "采购"),
    GoodsReceipt(FactStage.Fulfillment, 90, "到货"),

    ContractChange(FactStage.Change, 10, "增减项"),

    Acceptance(FactStage.Acceptance, 10, "阶段验收"),
    Rectification(FactStage.Acceptance, 20, "整改"),
    Reinspection(FactStage.Acceptance, 30, "复验"),

    Receivable(FactStage.Finance, 10, "应收"),
    Receipt(FactStage.Finance, 20, "收款"),
    Payable(FactStage.Finance, 30, "应付"),
    Payment(FactStage.Finance, 40, "付款"),
    Cost(FactStage.Finance, 50, "成本"),
    ProfitSettlement(FactStage.Finance, 60, "利润/结算"),

    Delivery(FactStage.AfterSales, 10, "交付"),
    Warranty(FactStage.AfterSales, 20, "质保/维保"),
    Feedback(FactStage.AfterSales, 30, "意见投诉"),
    Repurchase(FactStage.AfterSales, 40, "复购");

    private final FactStage stage;
    private final int order;
    private final String name;

    FactType(FactStage stage, int order, String name) {
        this.stage = stage;
        this.order = order;
        this.name = name;
    }

    public FactStage getStage() {
        return stage;
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
