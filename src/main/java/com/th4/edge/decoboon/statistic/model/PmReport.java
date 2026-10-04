package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/**
 * 履约驾驶舱报告。
 *
 * <p>项目经理只返回本人负责项目；企业负责人返回全公司项目履约汇总。</p>
 */
@Note("履约驾驶舱报告")
public class PmReport {

    private Integer projectCount = 0;
    private Integer preparingProjectCount = 0;
    private Integer ongoingProjectCount = 0;
    private Integer acceptingProjectCount = 0;
    private Integer suspendedProjectCount = 0;
    private Integer finishedProjectCount = 0;
    private Integer overdueProjectCount = 0;
    private Integer dueSoonProjectCount = 0;
    private Integer taskTodoCount = 0;
    private Integer taskDoingCount = 0;
    private Integer taskAcceptingCount = 0;
    private Integer taskOverdueCount = 0;
    private Integer reportTodayCount = 0;
    private Integer reportWeekCount = 0;
    private Integer pendingAcceptanceCount = 0;
    private Integer rejectedAcceptanceCount = 0;
    private Integer materialRiskCount = 0;
    private Integer materialDelayedCount = 0;
    private Integer customerCount = 0;
    private Integer quoteCount = 0;
    private BigDecimal contractAmount = BigDecimal.ZERO;
    private BigDecimal completedAmount = BigDecimal.ZERO;
    private BigDecimal averageProgress = BigDecimal.ZERO;
    private BigDecimal onTimeDeliveryRate = BigDecimal.ZERO;
    private BigDecimal acceptancePassRate = BigDecimal.ZERO;
    private Long noticeUnreadCount = 0L;
    private List<Project> focusProjects = List.of();
    private List<ProjectTask> urgentTasks = List.of();
    private List<Acceptance> pendingAcceptances = List.of();
    private List<MaterialPlan> materialRisks = List.of();
    private List<ProjectReport> recentReports = List.of();
    private List<Customer> recentCustomers = List.of();
    private List<Quote> recentQuotes = List.of();

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
    public Integer getOverdueProjectCount() { return overdueProjectCount; }
    public void setOverdueProjectCount(Integer overdueProjectCount) { this.overdueProjectCount = overdueProjectCount; }
    public Integer getDueSoonProjectCount() { return dueSoonProjectCount; }
    public void setDueSoonProjectCount(Integer dueSoonProjectCount) { this.dueSoonProjectCount = dueSoonProjectCount; }
    public Integer getTaskTodoCount() { return taskTodoCount; }
    public void setTaskTodoCount(Integer taskTodoCount) { this.taskTodoCount = taskTodoCount; }
    public Integer getTaskDoingCount() { return taskDoingCount; }
    public void setTaskDoingCount(Integer taskDoingCount) { this.taskDoingCount = taskDoingCount; }
    public Integer getTaskAcceptingCount() { return taskAcceptingCount; }
    public void setTaskAcceptingCount(Integer taskAcceptingCount) { this.taskAcceptingCount = taskAcceptingCount; }
    public Integer getTaskOverdueCount() { return taskOverdueCount; }
    public void setTaskOverdueCount(Integer taskOverdueCount) { this.taskOverdueCount = taskOverdueCount; }
    public Integer getReportTodayCount() { return reportTodayCount; }
    public void setReportTodayCount(Integer reportTodayCount) { this.reportTodayCount = reportTodayCount; }
    public Integer getReportWeekCount() { return reportWeekCount; }
    public void setReportWeekCount(Integer reportWeekCount) { this.reportWeekCount = reportWeekCount; }
    public Integer getPendingAcceptanceCount() { return pendingAcceptanceCount; }
    public void setPendingAcceptanceCount(Integer pendingAcceptanceCount) { this.pendingAcceptanceCount = pendingAcceptanceCount; }
    public Integer getRejectedAcceptanceCount() { return rejectedAcceptanceCount; }
    public void setRejectedAcceptanceCount(Integer rejectedAcceptanceCount) { this.rejectedAcceptanceCount = rejectedAcceptanceCount; }
    public Integer getMaterialRiskCount() { return materialRiskCount; }
    public void setMaterialRiskCount(Integer materialRiskCount) { this.materialRiskCount = materialRiskCount; }
    public Integer getMaterialDelayedCount() { return materialDelayedCount; }
    public void setMaterialDelayedCount(Integer materialDelayedCount) { this.materialDelayedCount = materialDelayedCount; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getCompletedAmount() { return completedAmount; }
    public void setCompletedAmount(BigDecimal completedAmount) { this.completedAmount = completedAmount; }
    public BigDecimal getAverageProgress() { return averageProgress; }
    public void setAverageProgress(BigDecimal averageProgress) { this.averageProgress = averageProgress; }
    public BigDecimal getOnTimeDeliveryRate() { return onTimeDeliveryRate; }
    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) { this.onTimeDeliveryRate = onTimeDeliveryRate; }
    public BigDecimal getAcceptancePassRate() { return acceptancePassRate; }
    public void setAcceptancePassRate(BigDecimal acceptancePassRate) { this.acceptancePassRate = acceptancePassRate; }
    public Long getNoticeUnreadCount() { return noticeUnreadCount; }
    public void setNoticeUnreadCount(Long noticeUnreadCount) { this.noticeUnreadCount = noticeUnreadCount; }
    public List<Project> getFocusProjects() { return focusProjects; }
    public void setFocusProjects(List<Project> focusProjects) { this.focusProjects = focusProjects; }
    public List<ProjectTask> getUrgentTasks() { return urgentTasks; }
    public void setUrgentTasks(List<ProjectTask> urgentTasks) { this.urgentTasks = urgentTasks; }
    public List<Acceptance> getPendingAcceptances() { return pendingAcceptances; }
    public void setPendingAcceptances(List<Acceptance> pendingAcceptances) { this.pendingAcceptances = pendingAcceptances; }
    public List<MaterialPlan> getMaterialRisks() { return materialRisks; }
    public void setMaterialRisks(List<MaterialPlan> materialRisks) { this.materialRisks = materialRisks; }
    public List<ProjectReport> getRecentReports() { return recentReports; }
    public void setRecentReports(List<ProjectReport> recentReports) { this.recentReports = recentReports; }
    public List<Customer> getRecentCustomers() { return recentCustomers; }
    public void setRecentCustomers(List<Customer> recentCustomers) { this.recentCustomers = recentCustomers; }
    public List<Quote> getRecentQuotes() { return recentQuotes; }
    public void setRecentQuotes(List<Quote> recentQuotes) { this.recentQuotes = recentQuotes; }
}
