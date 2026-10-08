package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 团队负责人经营与履约全景报告。 */
@Note("团队经营报告")
public class TeamLeaderReport {

    private String teamName;
    private Integer memberCount = 0;
    private Integer marketerCount = 0;
    private Integer designerCount = 0;
    private Integer projectManagerCount = 0;
    private Integer customerCount = 0;
    private Integer dormantCustomerCount = 0;
    private Integer followingCustomerCount = 0;
    private Integer pendingMeasureCount = 0;
    private Integer unassignedDesignerCount = 0;
    private Integer followDueCount = 0;
    private Integer followOverdueCount = 0;
    private Integer quoteCount = 0;
    private Integer pendingQuoteCount = 0;
    private Integer acceptedQuoteCount = 0;
    private BigDecimal quoteAmount = BigDecimal.ZERO;
    private BigDecimal acceptedQuoteAmount = BigDecimal.ZERO;
    private BigDecimal signedAmount = BigDecimal.ZERO;
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
    private List<TeamMemberPerformance> memberPerformances = List.of();
    private List<Customer> recentCustomers = List.of();
    private List<Quote> recentQuotes = List.of();
    private List<Project> recentProjects = List.of();

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public Integer getMarketerCount() { return marketerCount; }
    public void setMarketerCount(Integer marketerCount) { this.marketerCount = marketerCount; }
    public Integer getDesignerCount() { return designerCount; }
    public void setDesignerCount(Integer designerCount) { this.designerCount = designerCount; }
    public Integer getProjectManagerCount() { return projectManagerCount; }
    public void setProjectManagerCount(Integer projectManagerCount) { this.projectManagerCount = projectManagerCount; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getDormantCustomerCount() { return dormantCustomerCount; }
    public void setDormantCustomerCount(Integer dormantCustomerCount) { this.dormantCustomerCount = dormantCustomerCount; }
    public Integer getFollowingCustomerCount() { return followingCustomerCount; }
    public void setFollowingCustomerCount(Integer followingCustomerCount) { this.followingCustomerCount = followingCustomerCount; }
    public Integer getPendingMeasureCount() { return pendingMeasureCount; }
    public void setPendingMeasureCount(Integer pendingMeasureCount) { this.pendingMeasureCount = pendingMeasureCount; }
    public Integer getUnassignedDesignerCount() { return unassignedDesignerCount; }
    public void setUnassignedDesignerCount(Integer unassignedDesignerCount) { this.unassignedDesignerCount = unassignedDesignerCount; }
    public Integer getFollowDueCount() { return followDueCount; }
    public void setFollowDueCount(Integer followDueCount) { this.followDueCount = followDueCount; }
    public Integer getFollowOverdueCount() { return followOverdueCount; }
    public void setFollowOverdueCount(Integer followOverdueCount) { this.followOverdueCount = followOverdueCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getPendingQuoteCount() { return pendingQuoteCount; }
    public void setPendingQuoteCount(Integer pendingQuoteCount) { this.pendingQuoteCount = pendingQuoteCount; }
    public Integer getAcceptedQuoteCount() { return acceptedQuoteCount; }
    public void setAcceptedQuoteCount(Integer acceptedQuoteCount) { this.acceptedQuoteCount = acceptedQuoteCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getAcceptedQuoteAmount() { return acceptedQuoteAmount; }
    public void setAcceptedQuoteAmount(BigDecimal acceptedQuoteAmount) { this.acceptedQuoteAmount = acceptedQuoteAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
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
    public List<TeamMemberPerformance> getMemberPerformances() { return memberPerformances; }
    public void setMemberPerformances(List<TeamMemberPerformance> memberPerformances) { this.memberPerformances = memberPerformances; }
    public List<Customer> getRecentCustomers() { return recentCustomers; }
    public void setRecentCustomers(List<Customer> recentCustomers) { this.recentCustomers = recentCustomers; }
    public List<Quote> getRecentQuotes() { return recentQuotes; }
    public void setRecentQuotes(List<Quote> recentQuotes) { this.recentQuotes = recentQuotes; }
    public List<Project> getRecentProjects() { return recentProjects; }
    public void setRecentProjects(List<Project> recentProjects) { this.recentProjects = recentProjects; }
}
