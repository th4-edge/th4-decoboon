package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 设计师经营与履约报告。 */
@Note("设计师经营报告")
public class DesignerReport {

    private Integer customerCount = 0;
    private Integer followingCustomerCount = 0;
    private Integer signedCustomerCount = 0;
    private Integer quoteCount = 0;
    private Integer pendingQuoteCount = 0;
    private Integer acceptedQuoteCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;
    private BigDecimal conversionRate = BigDecimal.ZERO;
    private Integer projectCount = 0;
    private Integer ongoingProjectCount = 0;
    private Integer finishedProjectCount = 0;
    private Integer taskTodoCount = 0;
    private Integer taskDoneCount = 0;
    private Integer taskOverdueCount = 0;
    private Integer reportCount = 0;
    private Long noticeUnreadCount = 0L;
    private List<Customer> recentCustomers = List.of();
    private List<Quote> recentQuotes = List.of();
    private List<Project> recentProjects = List.of();

    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getFollowingCustomerCount() { return followingCustomerCount; }
    public void setFollowingCustomerCount(Integer followingCustomerCount) { this.followingCustomerCount = followingCustomerCount; }
    public Integer getSignedCustomerCount() { return signedCustomerCount; }
    public void setSignedCustomerCount(Integer signedCustomerCount) { this.signedCustomerCount = signedCustomerCount; }
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
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getOngoingProjectCount() { return ongoingProjectCount; }
    public void setOngoingProjectCount(Integer ongoingProjectCount) { this.ongoingProjectCount = ongoingProjectCount; }
    public Integer getFinishedProjectCount() { return finishedProjectCount; }
    public void setFinishedProjectCount(Integer finishedProjectCount) { this.finishedProjectCount = finishedProjectCount; }
    public Integer getTaskTodoCount() { return taskTodoCount; }
    public void setTaskTodoCount(Integer taskTodoCount) { this.taskTodoCount = taskTodoCount; }
    public Integer getTaskDoneCount() { return taskDoneCount; }
    public void setTaskDoneCount(Integer taskDoneCount) { this.taskDoneCount = taskDoneCount; }
    public Integer getTaskOverdueCount() { return taskOverdueCount; }
    public void setTaskOverdueCount(Integer taskOverdueCount) { this.taskOverdueCount = taskOverdueCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Long getNoticeUnreadCount() { return noticeUnreadCount; }
    public void setNoticeUnreadCount(Long noticeUnreadCount) { this.noticeUnreadCount = noticeUnreadCount; }
    public List<Customer> getRecentCustomers() { return recentCustomers; }
    public void setRecentCustomers(List<Customer> recentCustomers) { this.recentCustomers = recentCustomers; }
    public List<Quote> getRecentQuotes() { return recentQuotes; }
    public void setRecentQuotes(List<Quote> recentQuotes) { this.recentQuotes = recentQuotes; }
    public List<Project> getRecentProjects() { return recentProjects; }
    public void setRecentProjects(List<Project> recentProjects) { this.recentProjects = recentProjects; }
}
