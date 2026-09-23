package com.th4.edge.decoboon.plan.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 平台计划模板目录。 */
@Note("平台计划模板目录")
public class PlanTemplateCatalog {
    private Integer paymentPlanBaselineVersion = 1;
    private Integer constructionPlanBaselineVersion = 1;
    private List<PaymentPlanTemplate> paymentPlans = List.of();
    private List<ConstructionPlanTemplate> constructionPlans = List.of();

    public Integer getPaymentPlanBaselineVersion() { return paymentPlanBaselineVersion; }
    public void setPaymentPlanBaselineVersion(Integer paymentPlanBaselineVersion) { this.paymentPlanBaselineVersion = paymentPlanBaselineVersion; }
    public Integer getConstructionPlanBaselineVersion() { return constructionPlanBaselineVersion; }
    public void setConstructionPlanBaselineVersion(Integer constructionPlanBaselineVersion) { this.constructionPlanBaselineVersion = constructionPlanBaselineVersion; }
    public List<PaymentPlanTemplate> getPaymentPlans() { return paymentPlans; }
    public void setPaymentPlans(List<PaymentPlanTemplate> paymentPlans) { this.paymentPlans = paymentPlans == null ? List.of() : List.copyOf(paymentPlans); }
    public List<ConstructionPlanTemplate> getConstructionPlans() { return constructionPlans; }
    public void setConstructionPlans(List<ConstructionPlanTemplate> constructionPlans) { this.constructionPlans = constructionPlans == null ? List.of() : List.copyOf(constructionPlans); }
}
