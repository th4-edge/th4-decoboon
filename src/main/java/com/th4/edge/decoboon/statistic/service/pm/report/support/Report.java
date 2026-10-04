package com.th4.edge.decoboon.statistic.service.pm.report.support;

import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.model.PmReport;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 企业负责人全公司履约与项目经理个人履约报告实现。 */
@Note("履约驾驶舱报告")
@Description("企业负责人查看全公司项目履约，项目经理查看本人负责项目的施工、验收、材料、客户和业绩视图")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "quote", "designer", "manager",
                        "siteAddr", "area", "contractAmount", "status", "progress", "startDate",
                        "planEndDate", "actualEndDate", "currentStageName", "createTime" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "status", "owner", "designer", "createTime"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "customer", "createdBy", "area", "amount", "discount", "status", "createTime"}),
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "name", "zone", "craft", "spec", "quantity", "unit",
                        "status", "progress", "priority", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "sort", "status", "progress"}),
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "stage", "submittedBy", "status", "conclusion",
                        "issues", "evidenceUrls", "confirmedAt", "createTime"}),
        @AllowProperties(model = MaterialPlan.class,
                properties = {"planId", "project", "task", "material", "quantity", "unit", "status", "requiredAt", "arrivedAt", "createTime", "property" }),
        @AllowProperties(model = ProjectReport.class,
                properties = {"reportId", "project", "stage", "task", "type", "title", "content", "images",
                        "author", "authorRole", "visibleToCustomer", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/pm/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.pm.report.Report {

    private final TenantContext tenantContext;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectReportRepository reportRepository;
    private final NoticeRepository noticeRepository;

    public Report(TenantContext tenantContext,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  AcceptanceRepository acceptanceRepository,
                  MaterialPlanRepository materialPlanRepository,
                  ProjectReportRepository reportRepository,
                  NoticeRepository noticeRepository) {
        this.tenantContext = tenantContext;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.reportRepository = reportRepository;
        this.noticeRepository = noticeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PmReport call(@User Employee employee) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.Pm);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }

        Long comId = current.getCompany().getComId();
        List<Project> projects = current.getRole() == EmployeeRole.Boss
                ? projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId)
                : projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, current.getEmpId());

        PmReport report = new PmReport();
        report.setProjectCount(projects.size());
        report.setPreparingProjectCount(countProjects(projects, ProjectStatus.Preparing));
        report.setOngoingProjectCount(countProjects(projects, ProjectStatus.Ongoing));
        report.setAcceptingProjectCount(countProjects(projects, ProjectStatus.Accepting));
        report.setSuspendedProjectCount(countProjects(projects, ProjectStatus.Suspended));
        report.setFinishedProjectCount(countProjects(projects, ProjectStatus.Finished, ProjectStatus.Settled));
        report.setOverdueProjectCount((int) projects.stream()
                .filter(this::isProjectOverdue)
                .count());
        report.setDueSoonProjectCount((int) projects.stream()
                .filter(this::isProjectDueSoon)
                .count());
        report.setContractAmount(projects.stream()
                .map(Project::getContractAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setCompletedAmount(projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Finished
                        || project.getStatus() == ProjectStatus.Settled)
                .map(Project::getContractAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setAverageProgress(projects.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(projects.stream()
                        .map(Project::getProgress)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0))
                .setScale(1, RoundingMode.HALF_UP));
        report.setOnTimeDeliveryRate(onTimeDeliveryRate(projects));
        report.setNoticeUnreadCount(noticeRepository.countByReceiver_EmpIdAndReadFalse(current.getEmpId()));

        List<ProjectTask> allTasks = new ArrayList<>();
        List<Acceptance> allAcceptances = new ArrayList<>();
        List<MaterialPlan> allMaterials = new ArrayList<>();
        List<ProjectReport> allReports = new ArrayList<>();
        Map<Long, Customer> customers = new LinkedHashMap<>();
        Map<Long, Quote> quotes = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        for (Project project : projects) {
            List<ProjectTask> projectTasks = taskRepository.findByProject(project);
            allTasks.addAll(projectTasks);
            allMaterials.addAll(materialPlanRepository.findByProject(project));
            allAcceptances.addAll(acceptanceRepository
                    .findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId(), PageRequest.of(0, 50))
                    .getContent());
            List<ProjectReport> projectReports = reportRepository
                    .findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
                            comId, project.getProjectId(), PageRequest.of(0, 30))
                    .getContent();
            allReports.addAll(projectReports);
            report.setReportTodayCount(report.getReportTodayCount() + countReportsOn(projectReports, today));
            report.setReportWeekCount(report.getReportWeekCount() + countReportsSince(projectReports, weekStart));
            if (project.getCustomer() != null) {
                customers.put(project.getCustomer().getCustId(), project.getCustomer());
            }
            if (project.getQuote() != null) {
                quotes.put(project.getQuote().getQuoteId(), project.getQuote());
            }
        }

        int todo = 0;
        int doing = 0;
        int accepting = 0;
        int overdue = 0;
        List<ProjectTask> urgentTasks = new ArrayList<>();
        for (ProjectTask task : allTasks) {
            if (task.getStatus() == TaskStatus.Todo) todo++;
            if (task.getStatus() == TaskStatus.Doing) doing++;
            if (task.getStatus() == TaskStatus.Accepting) accepting++;
            if (isTaskOverdue(task)) overdue++;
            if (isTaskOpen(task) && task.getPlannedEnd() != null && !task.getPlannedEnd().isAfter(today.plusDays(3))) {
                urgentTasks.add(task);
            }
        }
        report.setTaskTodoCount(todo);
        report.setTaskDoingCount(doing);
        report.setTaskAcceptingCount(accepting);
        report.setTaskOverdueCount(overdue);
        urgentTasks.sort(Comparator
                .comparing((ProjectTask task) -> isTaskOverdue(task) ? 0 : 1)
                .thenComparing(ProjectTask::getPlannedEnd));
        report.setUrgentTasks(urgentTasks.stream().limit(20).toList());

        long pendingAcceptanceCount = allAcceptances.stream()
                .filter(item -> item.getStatus() == AcceptStatus.Pending)
                .count();
        long rejectedAcceptanceCount = allAcceptances.stream()
                .filter(item -> item.getStatus() == AcceptStatus.Rejected)
                .count();
        long confirmedAcceptanceCount = allAcceptances.stream()
                .filter(item -> item.getStatus() == AcceptStatus.Confirmed)
                .count();
        report.setPendingAcceptanceCount((int) pendingAcceptanceCount);
        report.setRejectedAcceptanceCount((int) rejectedAcceptanceCount);
        report.setAcceptancePassRate(rate(confirmedAcceptanceCount, confirmedAcceptanceCount + rejectedAcceptanceCount));
        report.setPendingAcceptances(allAcceptances.stream()
                .filter(item -> item.getStatus() == AcceptStatus.Pending || item.getStatus() == AcceptStatus.Rejected)
                .sorted(Comparator.comparing(Acceptance::getAcceptanceId).reversed())
                .limit(20)
                .toList());

        List<MaterialPlan> materialRisks = allMaterials.stream()
                .filter(this::isMaterialRisk)
                .sorted(Comparator.comparing(MaterialPlan::getRequiredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        report.setMaterialRiskCount(materialRisks.size());
        report.setMaterialDelayedCount((int) materialRisks.stream().filter(this::isMaterialDelayed).count());
        report.setMaterialRisks(materialRisks.stream().limit(20).toList());

        allReports.sort(Comparator.comparing(ProjectReport::getCreateTime,
                Comparator.nullsLast(Comparator.comparing(com.th4.edge.framework.data.date.model.DateTime::getDateTime))).reversed());
        report.setRecentReports(allReports.stream().limit(12).toList());

        report.setCustomerCount(customers.size());
        report.setQuoteCount(quotes.size());
        report.setRecentCustomers(customers.values().stream()
                .sorted(Comparator.comparing(Customer::getCustId).reversed())
                .limit(12)
                .toList());
        report.setRecentQuotes(quotes.values().stream()
                .sorted(Comparator.comparing(Quote::getQuoteId).reversed())
                .limit(12)
                .toList());
        report.setFocusProjects(projects.stream()
                .sorted(Comparator.comparingInt(this::projectPriority)
                        .thenComparing(Project::getPlanEndDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(12)
                .toList());
        return report;
    }

    private int countProjects(List<Project> projects, ProjectStatus... statuses) {
        int result = 0;
        for (Project project : projects) {
            for (ProjectStatus status : statuses) {
                if (project.getStatus() == status) {
                    result++;
                    break;
                }
            }
        }
        return result;
    }

    private int projectPriority(Project project) {
        if (project.getStatus() == ProjectStatus.Suspended) return 0;
        if (isProjectOverdue(project)) return 1;
        if (project.getStatus() == ProjectStatus.Accepting) return 2;
        if (project.getStatus() == ProjectStatus.Ongoing) return 3;
        if (project.getStatus() == ProjectStatus.Preparing) return 4;
        return 5;
    }

    private boolean isProjectOverdue(Project project) {
        return project.getPlanEndDate() != null
                && project.getPlanEndDate().isBefore(LocalDate.now())
                && project.getStatus() != ProjectStatus.Finished
                && project.getStatus() != ProjectStatus.Settled;
    }

    private boolean isProjectDueSoon(Project project) {
        return project.getPlanEndDate() != null
                && !project.getPlanEndDate().isBefore(LocalDate.now())
                && !project.getPlanEndDate().isAfter(LocalDate.now().plusDays(3))
                && project.getStatus() != ProjectStatus.Finished
                && project.getStatus() != ProjectStatus.Settled;
    }

    private boolean isTaskOpen(ProjectTask task) {
        return task.getStatus() != TaskStatus.Done && task.getStatus() != TaskStatus.Cancelled;
    }

    private boolean isTaskOverdue(ProjectTask task) {
        return task.getPlannedEnd() != null
                && task.getPlannedEnd().isBefore(LocalDate.now())
                && isTaskOpen(task);
    }

    private boolean isMaterialRisk(MaterialPlan plan) {
        if (plan.getStatus() == MaterialPlanStatus.Arrived || plan.getStatus() == MaterialPlanStatus.Closed) {
            return false;
        }
        return plan.getRequiredAt() == null || !plan.getRequiredAt().isAfter(LocalDate.now().plusDays(3));
    }

    private boolean isMaterialDelayed(MaterialPlan plan) {
        return plan.getRequiredAt() != null
                && plan.getRequiredAt().isBefore(LocalDate.now())
                && plan.getStatus() != MaterialPlanStatus.Arrived
                && plan.getStatus() != MaterialPlanStatus.Closed;
    }

    private int countReportsOn(List<ProjectReport> reports, LocalDate date) {
        return (int) reports.stream()
                .filter(item -> item.getCreateTime() != null && date.equals(item.getCreateTime().toLocalDate()))
                .count();
    }

    private int countReportsSince(List<ProjectReport> reports, LocalDate date) {
        return (int) reports.stream()
                .filter(item -> item.getCreateTime() != null && !item.getCreateTime().toLocalDate().isBefore(date))
                .count();
    }

    private BigDecimal onTimeDeliveryRate(List<Project> projects) {
        List<Project> finished = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Finished
                        || project.getStatus() == ProjectStatus.Settled)
                .filter(project -> project.getPlanEndDate() != null && project.getActualEndDate() != null)
                .toList();
        if (finished.isEmpty()) return BigDecimal.ZERO;
        long onTime = finished.stream()
                .filter(project -> !project.getActualEndDate().isAfter(project.getPlanEndDate()))
                .count();
        return rate(onTime, finished.size());
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
