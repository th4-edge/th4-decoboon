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
}
