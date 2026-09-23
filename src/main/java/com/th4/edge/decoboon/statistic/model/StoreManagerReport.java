package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 店长经营与履约全景报告。 */
@Note("门店经营报告")
public class StoreManagerReport {

    private String storeName;
    private Integer memberCount = 0;
    private Integer designerCount = 0;
    private Integer marketerCount = 0;
    private Integer projectManagerCount = 0;
    private Integer customerCount = 0;
    private Integer newCustomerCount = 0;
    private Integer followingCustomerCount = 0;
    private Integer pendingMeasureCount = 0;
    private Integer unassignedDesignerCount = 0;
    private Integer quoteCount = 0;
    private Integer pendingQuoteCount = 0;
    private Integer acceptedQuoteCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;
    private BigDecimal conversionRate = BigDecimal.ZERO;
    private Integer projectCount = 0;
    private Integer preparingProjectCount = 0;
    private Integer ongoingProjectCount = 0;
    private Integer acceptingProjectCount = 0;
    private Integer suspendedProjectCount = 0;
    private Integer finishedProjectCount = 0;
    private Integer taskTodoCount = 0;
    private Integer taskOverdueCount = 0;
    private Integer reportCount = 0;
    private Long noticeUnreadCount = 0L;
    private List<DesignerPerformance> designerPerformances = List.of();
    private List<Customer> recentCustomers = List.of();
    private List<Quote> recentQuotes = List.of();
    private List<Project> recentProjects = List.of();

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public Integer getDesignerCount() { return designerCount; }
    public void setDesignerCount(Integer designerCount) { this.designerCount = designerCount; }
    public Integer getMarketerCount() { return marketerCount; }
    public void setMarketerCount(Integer marketerCount) { this.marketerCount = marketerCount; }
    public Integer getProjectManagerCount() { return projectManagerCount; }
    public void setProjectManagerCount(Integer projectManagerCount) { this.projectManagerCount = projectManagerCount; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getNewCustomerCount() { return newCustomerCount; }
    public void setNewCustomerCount(Integer newCustomerCount) { this.newCustomerCount = newCustomerCount; }
    public Integer getFollowingCustomerCount() { return followingCustomerCount; }
    public void setFollowingCustomerCount(Integer followingCustomerCount) { this.followingCustomerCount = followingCustomerCount; }
    public Integer getPendingMeasureCount() { return pendingMeasureCount; }
    public void setPendingMeasureCount(Integer pendingMeasureCount) { this.pendingMeasureCount = pendingMeasureCount; }
    public Integer getUnassignedDesignerCount() { return unassignedDesignerCount; }
    public void setUnassignedDesignerCount(Integer unassignedDesignerCount) { this.unassignedDesignerCount = unassignedDesignerCount; }
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
    public Integer getPreparingProjectCount() { return preparingProjectCount; }
    public void setPreparingProjectCount(Integer preparingProjectCount) { this.preparingProjectCount = preparingProjectCount; }
    public Integer getOngoingProjectCount() { return ongoingProjectCount; }
    public void setOngoingProjectCount(Integer ongoingProjectCount) { this.ongoingProjectCount = ongoingProjectCount; }
    public Integer getAcceptingProjectCount() { return acceptingProjectCount; }
    public void setAcceptingProjectCount(Integer acceptingProjectCount) { this.acceptingProjectCount = acceptingProjectCount; }
    public Integer getSuspendedProjectCount() { return suspendedProjectCount; }
    public void setSuspendedProjectCount(Integer suspendedProjectCount) { this.suspendedProjectCount = suspendedProjectCount; }
    public Integer getFinishedProjectCount() { return finishedProjectCount; }
    public void setFinishedProjectCount(Integer finishedProjectCount) { this.finishedProjectCount = finishedProjectCount; }
    public Integer getTaskTodoCount() { return taskTodoCount; }
    public void setTaskTodoCount(Integer taskTodoCount) { this.taskTodoCount = taskTodoCount; }
    public Integer getTaskOverdueCount() { return taskOverdueCount; }
    public void setTaskOverdueCount(Integer taskOverdueCount) { this.taskOverdueCount = taskOverdueCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Long getNoticeUnreadCount() { return noticeUnreadCount; }
    public void setNoticeUnreadCount(Long noticeUnreadCount) { this.noticeUnreadCount = noticeUnreadCount; }
    public List<DesignerPerformance> getDesignerPerformances() { return designerPerformances; }
    public void setDesignerPerformances(List<DesignerPerformance> designerPerformances) { this.designerPerformances = designerPerformances; }
    public List<Customer> getRecentCustomers() { return recentCustomers; }
    public void setRecentCustomers(List<Customer> recentCustomers) { this.recentCustomers = recentCustomers; }
    public List<Quote> getRecentQuotes() { return recentQuotes; }
    public void setRecentQuotes(List<Quote> recentQuotes) { this.recentQuotes = recentQuotes; }
    public List<Project> getRecentProjects() { return recentProjects; }
    public void setRecentProjects(List<Project> recentProjects) { this.recentProjects = recentProjects; }
}
