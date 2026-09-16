package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

@Note("业务员经营报告")
public class MarketerReport {

    private Integer customerCount = 0;
    private Integer newCustomerCount = 0;
    private Integer followingCustomerCount = 0;
    private Integer signedCustomerCount = 0;
    private Integer pendingMeasureCount = 0;
    private Integer quotedCustomerCount = 0;
    private Integer followDueCount = 0;
    private Integer followOverdueCount = 0;
    private Integer draftQuoteCount = 0;
    private Integer submittedQuoteCount = 0;
    private Integer lostCustomerCount = 0;
    private Integer projectCount = 0;
    private Integer ongoingProjectCount = 0;
    private Integer finishedProjectCount = 0;
    private Integer taskTodoCount = 0;
    private Integer taskOverdueCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal acceptedQuoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;
    private Integer wechatAddedCount = 0;
    private Integer convertedCustomerCount = 0;
    private BigDecimal conversionRate = BigDecimal.ZERO;
    private BigDecimal averageConversionDays = BigDecimal.ZERO;
    private Integer goldenHotCount = 0;
    private Integer goldenWarmCount = 0;
    private Integer goldenCoolingCount = 0;
    private Integer goldenRiskCount = 0;
    private List<Customer> recentCustomers = List.of();
    private List<Project> recentProjects = List.of();

    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getNewCustomerCount() { return newCustomerCount; }
    public void setNewCustomerCount(Integer newCustomerCount) { this.newCustomerCount = newCustomerCount; }
    public Integer getFollowingCustomerCount() { return followingCustomerCount; }
    public void setFollowingCustomerCount(Integer followingCustomerCount) { this.followingCustomerCount = followingCustomerCount; }
    public Integer getSignedCustomerCount() { return signedCustomerCount; }
    public void setSignedCustomerCount(Integer signedCustomerCount) { this.signedCustomerCount = signedCustomerCount; }
    public Integer getPendingMeasureCount() { return pendingMeasureCount; }
    public void setPendingMeasureCount(Integer pendingMeasureCount) { this.pendingMeasureCount = pendingMeasureCount; }
    public Integer getQuotedCustomerCount() { return quotedCustomerCount; }
    public void setQuotedCustomerCount(Integer quotedCustomerCount) { this.quotedCustomerCount = quotedCustomerCount; }
    public Integer getFollowDueCount() { return followDueCount; }
    public void setFollowDueCount(Integer followDueCount) { this.followDueCount = followDueCount; }
    public Integer getFollowOverdueCount() { return followOverdueCount; }
    public void setFollowOverdueCount(Integer followOverdueCount) { this.followOverdueCount = followOverdueCount; }
    public Integer getDraftQuoteCount() { return draftQuoteCount; }
    public void setDraftQuoteCount(Integer draftQuoteCount) { this.draftQuoteCount = draftQuoteCount; }
    public Integer getSubmittedQuoteCount() { return submittedQuoteCount; }
    public void setSubmittedQuoteCount(Integer submittedQuoteCount) { this.submittedQuoteCount = submittedQuoteCount; }
    public Integer getLostCustomerCount() { return lostCustomerCount; }
    public void setLostCustomerCount(Integer lostCustomerCount) { this.lostCustomerCount = lostCustomerCount; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getOngoingProjectCount() { return ongoingProjectCount; }
    public void setOngoingProjectCount(Integer ongoingProjectCount) { this.ongoingProjectCount = ongoingProjectCount; }
    public Integer getFinishedProjectCount() { return finishedProjectCount; }
    public void setFinishedProjectCount(Integer finishedProjectCount) { this.finishedProjectCount = finishedProjectCount; }
    public Integer getTaskTodoCount() { return taskTodoCount; }
    public void setTaskTodoCount(Integer taskTodoCount) { this.taskTodoCount = taskTodoCount; }
    public Integer getTaskOverdueCount() { return taskOverdueCount; }
    public void setTaskOverdueCount(Integer taskOverdueCount) { this.taskOverdueCount = taskOverdueCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getAcceptedQuoteAmount() { return acceptedQuoteAmount; }
    public void setAcceptedQuoteAmount(BigDecimal acceptedQuoteAmount) { this.acceptedQuoteAmount = acceptedQuoteAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public List<Customer> getRecentCustomers() { return recentCustomers; }
    public void setRecentCustomers(List<Customer> recentCustomers) { this.recentCustomers = recentCustomers; }
    public List<Project> getRecentProjects() { return recentProjects; }
    public void setRecentProjects(List<Project> recentProjects) { this.recentProjects = recentProjects; }
    public Integer getWechatAddedCount() { return wechatAddedCount; }
    public void setWechatAddedCount(Integer wechatAddedCount) { this.wechatAddedCount = wechatAddedCount; }
    public Integer getConvertedCustomerCount() { return convertedCustomerCount; }
    public void setConvertedCustomerCount(Integer convertedCustomerCount) { this.convertedCustomerCount = convertedCustomerCount; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    public BigDecimal getAverageConversionDays() { return averageConversionDays; }
    public void setAverageConversionDays(BigDecimal averageConversionDays) { this.averageConversionDays = averageConversionDays; }
    public Integer getGoldenHotCount() { return goldenHotCount; }
    public void setGoldenHotCount(Integer goldenHotCount) { this.goldenHotCount = goldenHotCount; }
    public Integer getGoldenWarmCount() { return goldenWarmCount; }
    public void setGoldenWarmCount(Integer goldenWarmCount) { this.goldenWarmCount = goldenWarmCount; }
    public Integer getGoldenCoolingCount() { return goldenCoolingCount; }
    public void setGoldenCoolingCount(Integer goldenCoolingCount) { this.goldenCoolingCount = goldenCoolingCount; }
    public Integer getGoldenRiskCount() { return goldenRiskCount; }
    public void setGoldenRiskCount(Integer goldenRiskCount) { this.goldenRiskCount = goldenRiskCount; }
}
