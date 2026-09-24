package com.th4.edge.decoboon.statistic.service.teamleader.report.support;

import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.context.QuoteAcceptanceContext;
import com.th4.edge.decoboon.statistic.model.TeamLeaderReport;
import com.th4.edge.decoboon.statistic.model.TeamMemberPerformance;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
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
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 团队负责人经营报告，所有数据严格限定在登录负责人所属企业、团队。 */
@Note("团队经营报告")
@Description("聚合团队成员、客户、报价、项目、计划风险、现场动态与通知")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr",
                        "area", "budget", "status", "owner", "designer", "wechatAdded", "visitCount",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "handedOffAt", "createTime"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "customer", "createdBy", "area", "amount", "cost",
                        "discount", "status", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "designer", "manager", "siteAddr",
                        "area", "contractAmount", "cost", "status", "progress", "startDate", "planEndDate", "createTime" }),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("statistic/team-leader/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.teamleader.report.Report {

    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectReportRepository projectReportRepository;
    private final NoticeRepository noticeRepository;

    public Report(TenantContext tenantContext,
                  EmployeeRepository employeeRepository,
                  CustomerRepository customerRepository,
                  QuoteRepository quoteRepository,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  ProjectReportRepository projectReportRepository,
                  NoticeRepository noticeRepository) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectReportRepository = projectReportRepository;
        this.noticeRepository = noticeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamLeaderReport call(@User Employee employee) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.TeamLeader);
        if (current.getCompany() == null || current.getTeam() == null
                || current.getTeam().getTeamId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前团队负责人未归属企业或团队"));
        }

        Long comId = current.getCompany().getComId();
        Long teamId = current.getTeam().getTeamId();
        List<Employee> members = employeeRepository
                .findByCompany_ComIdAndTeam_TeamIdOrderByName(comId, teamId);
        List<Customer> customers = customerRepository
                .findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(comId, teamId);
        List<Quote> quotes = quoteRepository
                .findByCompany_ComIdAndCustomer_Team_TeamIdOrderByQuoteIdDesc(comId, teamId);
        List<Project> projects = projectRepository
                .findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(comId, teamId);

        TeamLeaderReport report = new TeamLeaderReport();
        report.setTeamName(current.getTeam().getName());
        report.setMemberCount(members.size());
        report.setMarketerCount(countRole(members, EmployeeRole.Marketer));
        report.setDesignerCount(countRole(members, EmployeeRole.Designer));
        report.setProjectManagerCount(countRole(members, EmployeeRole.Pm));
        report.setCustomerCount(customers.size());
        report.setFollowingCustomerCount(countCustomers(customers,
                CustStatus.Following, CustStatus.Quoted, CustStatus.PendingMeasure));
        report.setPendingMeasureCount(countCustomers(customers, CustStatus.PendingMeasure));
        report.setUnassignedDesignerCount((int) customers.stream()
                .filter(customer -> customer.getStatus() == CustStatus.PendingMeasure
                        && customer.getMeasureAt() != null
                        && customer.getDesigner() == null)
                .count());
        report.setFollowDueCount(countFollow(customers, 0));
        report.setFollowOverdueCount(countFollow(customers, 1));
        report.setQuoteCount(quotes.size());
        report.setPendingQuoteCount((int) quotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Draft
                        || quote.getStatus() == QuoteStatus.Submitted)
                .count());
        report.setAcceptedQuoteCount((int) quotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .count());
        report.setQuoteAmount(quotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Rejected && quote.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setAcceptedQuoteAmount(quotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setSignedAmount(projects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        QuoteAcceptanceContext.Result acceptance = QuoteAcceptanceContext.analyze(quotes);
        report.setSubmittedQuoteCount(acceptance.submittedQuoteCount());
        report.setPendingDecisionQuoteCount(acceptance.pendingDecisionQuoteCount());
        report.setDecidedOpportunityCount(acceptance.decidedOpportunityCount());
        report.setAcceptedOpportunityCount(acceptance.acceptedOpportunityCount());
        report.setFinalQuoteAcceptanceRate(acceptance.finalAcceptanceRate());
        report.setFirstQuoteAcceptanceRate(acceptance.firstAcceptanceRate());
        report.setConversionRate(acceptance.finalAcceptanceRate());
        report.setProjectCount(projects.size());
        report.setPreparingProjectCount(countProjects(projects, ProjectStatus.Preparing));
        report.setOngoingProjectCount(countProjects(projects, ProjectStatus.Ongoing));
        report.setAcceptingProjectCount(countProjects(projects, ProjectStatus.Accepting));
        report.setSuspendedProjectCount(countProjects(projects, ProjectStatus.Suspended));
        report.setFinishedProjectCount(countProjects(projects, ProjectStatus.Finished, ProjectStatus.Settled));

        int todo = 0;
        int overdue = 0;
        int reportCount = 0;
        for (Project project : projects) {
            for (ProjectTask task : taskRepository.findByProject(project)) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) {
                    todo++;
                }
                if (isOverdue(task)) {
                    overdue++;
                }
            }
            reportCount += projectReportRepository.countByProject_ProjectId(project.getProjectId());
        }
        report.setTaskTodoCount(todo);
        report.setTaskOverdueCount(overdue);
        report.setReportCount(reportCount);
        report.setNoticeUnreadCount(noticeRepository.countByReceiver_EmpIdAndReadFalse(current.getEmpId()));
        report.setMemberPerformances(members.stream()
                .map(member -> performance(member, customers, quotes, projects))
                .sorted(Comparator.comparing(TeamMemberPerformance::getTaskOverdueCount).reversed()
                        .thenComparing(TeamMemberPerformance::getSignedAmount, Comparator.reverseOrder()))
                .toList());
        report.setRecentCustomers(customers.stream().limit(5).toList());
        report.setRecentQuotes(quotes.stream().limit(5).toList());
        report.setRecentProjects(projects.stream().limit(5).toList());
        return report;
    }

    private TeamMemberPerformance performance(Employee member,
                                              List<Customer> customers,
                                              List<Quote> quotes,
                                              List<Project> projects) {
        Long empId = member.getEmpId();
        List<Customer> memberCustomers = customers.stream()
                .filter(customer -> customer.getOwner() != null
                        && empId.equals(customer.getOwner().getEmpId()))
                .toList();
        List<Quote> memberQuotes = quotes.stream()
                .filter(quote -> quote.getCreatedBy() != null
                        && empId.equals(quote.getCreatedBy().getEmpId()))
                .toList();
        List<Project> memberProjects = projects.stream()
                .filter(project -> belongsTo(project, member))
                .toList();

        TeamMemberPerformance performance = new TeamMemberPerformance();
        performance.setEmployee(member);
        performance.setCustomerCount(memberCustomers.size());
        performance.setFollowDueCount(countFollow(memberCustomers, 0));
        performance.setFollowOverdueCount(countFollow(memberCustomers, 1));
        performance.setQuoteCount(memberQuotes.size());
        performance.setPendingQuoteCount((int) memberQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Draft
                        || quote.getStatus() == QuoteStatus.Submitted)
                .count());
        performance.setAcceptedQuoteCount((int) memberQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .count());
        performance.setQuoteAmount(memberQuotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Rejected && quote.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setSignedAmount(memberProjects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setProjectCount(memberProjects.size());
        performance.setOngoingProjectCount((int) memberProjects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Preparing
                        || project.getStatus() == ProjectStatus.Ongoing
                        || project.getStatus() == ProjectStatus.Accepting)
                .count());
        QuoteAcceptanceContext.Result acceptance = QuoteAcceptanceContext.analyze(quotes, member);
        performance.setSubmittedQuoteCount(acceptance.submittedQuoteCount());
        performance.setPendingDecisionQuoteCount(acceptance.pendingDecisionQuoteCount());
        performance.setDecidedOpportunityCount(acceptance.decidedOpportunityCount());
        performance.setAcceptedOpportunityCount(acceptance.acceptedOpportunityCount());
        performance.setFinalQuoteAcceptanceRate(acceptance.finalAcceptanceRate());
        performance.setFirstQuoteAcceptanceRate(acceptance.firstAcceptanceRate());
        performance.setConversionRate(acceptance.finalAcceptanceRate());

        int todo = 0;
        int overdue = 0;
        int reports = 0;
        for (Project project : memberProjects) {
            for (ProjectTask task : taskRepository.findByProject(project)) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) {
                    todo++;
                }
                if (isOverdue(task)) {
                    overdue++;
                }
            }
            reports += projectReportRepository.countByProject_ProjectId(project.getProjectId());
        }
        performance.setTaskTodoCount(todo);
        performance.setTaskOverdueCount(overdue);
        performance.setReportCount(reports);
        return performance;
    }

    private boolean belongsTo(Project project, Employee member) {
        if (member.getRole() == EmployeeRole.TeamLeader) {
            return true;
        }
        return isEmployee(project.getDesigner(), member)
                || isEmployee(project.getManager(), member)
                || (project.getQuote() != null && isEmployee(project.getQuote().getCreatedBy(), member));
    }

    private boolean isEmployee(Employee target, Employee member) {
        return target != null && member.getEmpId() != null
                && member.getEmpId().equals(target.getEmpId());
    }

    private boolean isOverdue(ProjectTask task) {
        return task.getPlannedEnd() != null
                && task.getPlannedEnd().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.Done
                && task.getStatus() != TaskStatus.Cancelled;
    }

    private int countFollow(List<Customer> customers, int offset) {
        LocalDate today = LocalDate.now();
        return (int) customers.stream().filter(customer -> {
            if (customer.getNextFollowAt() == null
                    || customer.getStatus() == CustStatus.Signed
                    || customer.getStatus() == CustStatus.Serving
                    || customer.getStatus() == CustStatus.Finished
                    || customer.getStatus() == CustStatus.Lost) {
                return false;
            }
            LocalDate next = customer.getNextFollowAt().toLocalDate();
            return offset == 0 ? next.isEqual(today) : next.isBefore(today);
        }).count();
    }

    private int countRole(List<Employee> members, EmployeeRole role) {
        return (int) members.stream().filter(member -> member.getRole() == role).count();
    }

    private int countCustomers(List<Customer> customers, CustStatus... statuses) {
        return (int) customers.stream().filter(customer -> contains(statuses, customer.getStatus())).count();
    }

    private int countProjects(List<Project> projects, ProjectStatus... statuses) {
        return (int) projects.stream().filter(project -> contains(statuses, project.getStatus())).count();
    }

    private <T> boolean contains(T[] values, T target) {
        for (T value : values) {
            if (value == target) return true;
        }
        return false;
    }

}
