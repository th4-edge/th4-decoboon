package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;

import java.math.BigDecimal;

/** 门店设计师工作量与经营表现。 */
@Note("设计师经营表现")
public class DesignerPerformance {

    private Employee designer;
    private Integer customerCount = 0;
    private Integer pendingMeasureCount = 0;
    private Integer quoteCount = 0;
    private Integer pendingQuoteCount = 0;
    private Integer acceptedQuoteCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;
    private Integer projectCount = 0;
    private Integer ongoingProjectCount = 0;
    private Integer taskTodoCount = 0;
    private Integer taskOverdueCount = 0;
    private Integer reportCount = 0;
    private BigDecimal conversionRate = BigDecimal.ZERO;
    /** 已提交报价量：排除草稿与已被替代报价（MET-001）。 */
    private Integer submittedQuoteCount = 0;
    /** 待决策报价量：已提交且尚未形成客户决策，单独展示且不进入分母（MET-001）。 */
    private Integer pendingDecisionQuoteCount = 0;
    /** 已产生明确客户决策的报价机会数（客户 + 房屋）。 */
    private Integer decidedOpportunityCount = 0;
    /** 最终被接受或已转项目的报价机会数。 */
    private Integer acceptedOpportunityCount = 0;
    /** 最终报价接受率（MET-001 主指标）。 */
    private BigDecimal finalQuoteAcceptanceRate = BigDecimal.ZERO;
    /** 首版报价接受率（MET-001 辅助指标）。 */
    private BigDecimal firstQuoteAcceptanceRate = BigDecimal.ZERO;

    public Employee getDesigner() { return designer; }
    public void setDesigner(Employee designer) { this.designer = designer; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getPendingMeasureCount() { return pendingMeasureCount; }
    public void setPendingMeasureCount(Integer pendingMeasureCount) { this.pendingMeasureCount = pendingMeasureCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getPendingQuoteCount() { return pendingQuoteCount; }
    public void setPendingQuoteCount(Integer pendingQuoteCount) { this.pendingQuoteCount = pendingQuoteCount; }
    public Integer getAcceptedQuoteCount() { return acceptedQuoteCount; }
    public void setAcceptedQuoteCount(Integer acceptedQuoteCount) { this.acceptedQuoteCount = acceptedQuoteCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getOngoingProjectCount() { return ongoingProjectCount; }
    public void setOngoingProjectCount(Integer ongoingProjectCount) { this.ongoingProjectCount = ongoingProjectCount; }
    public Integer getTaskTodoCount() { return taskTodoCount; }
    public void setTaskTodoCount(Integer taskTodoCount) { this.taskTodoCount = taskTodoCount; }
    public Integer getTaskOverdueCount() { return taskOverdueCount; }
    public void setTaskOverdueCount(Integer taskOverdueCount) { this.taskOverdueCount = taskOverdueCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    public Integer getSubmittedQuoteCount() { return submittedQuoteCount; }
    public void setSubmittedQuoteCount(Integer submittedQuoteCount) { this.submittedQuoteCount = submittedQuoteCount; }
    public Integer getPendingDecisionQuoteCount() { return pendingDecisionQuoteCount; }
    public void setPendingDecisionQuoteCount(Integer pendingDecisionQuoteCount) { this.pendingDecisionQuoteCount = pendingDecisionQuoteCount; }
    public Integer getDecidedOpportunityCount() { return decidedOpportunityCount; }
    public void setDecidedOpportunityCount(Integer decidedOpportunityCount) { this.decidedOpportunityCount = decidedOpportunityCount; }
    public Integer getAcceptedOpportunityCount() { return acceptedOpportunityCount; }
    public void setAcceptedOpportunityCount(Integer acceptedOpportunityCount) { this.acceptedOpportunityCount = acceptedOpportunityCount; }
    public BigDecimal getFinalQuoteAcceptanceRate() { return finalQuoteAcceptanceRate; }
    public void setFinalQuoteAcceptanceRate(BigDecimal finalQuoteAcceptanceRate) { this.finalQuoteAcceptanceRate = finalQuoteAcceptanceRate; }
    public BigDecimal getFirstQuoteAcceptanceRate() { return firstQuoteAcceptanceRate; }
    public void setFirstQuoteAcceptanceRate(BigDecimal firstQuoteAcceptanceRate) { this.firstQuoteAcceptanceRate = firstQuoteAcceptanceRate; }
}
