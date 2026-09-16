package com.th4.edge.decoboon.statistic.service.store.report.support;

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
import com.th4.edge.decoboon.statistic.model.DesignerPerformance;
import com.th4.edge.decoboon.statistic.model.StoreManagerReport;
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
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 门店店长经营报告，所有数据严格限定在登录店长所属企业、门店。 */
@Note("门店经营报告")
@Description("聚合门店成员、客户、报价、项目、施工风险、通知与设计师工作量")
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
                properties = {"empId", "name", "telNo", "role", "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("statistic/store/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.store.report.Report {

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
    public StoreManagerReport call(@User Employee employee) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.StoreManager);
        if (current.getCompany() == null || current.getStore() == null
                || current.getStore().getStoreId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前店长未归属企业或门店"));
        }

        Long comId = current.getCompany().getComId();
        Long storeId = current.getStore().getStoreId();
        List<Employee> members = employeeRepository
                .findByCompany_ComIdAndStore_StoreIdOrderByName(comId, storeId);
        List<Customer> customers = customerRepository
                .findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, storeId);
        List<Quote> quotes = quoteRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(comId, storeId);
        List<Project> projects = projectRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(comId, storeId);

        StoreManagerReport report = new StoreManagerReport();
        report.setStoreName(current.getStore().getName());
        report.setMemberCount(members.size());
        report.setDesignerCount(countRole(members, EmployeeRole.Designer));
        report.setMarketerCount(countRole(members, EmployeeRole.Marketer));
        report.setProjectManagerCount(countRole(members, EmployeeRole.Pm));
        report.setCustomerCount(customers.size());
        report.setNewCustomerCount(countCustomers(customers, CustStatus.New));
        report.setFollowingCustomerCount(countCustomers(customers,
                CustStatus.Following, CustStatus.Quoted));
        report.setPendingMeasureCount(countCustomers(customers, CustStatus.PendingMeasure));
        report.setUnassignedDesignerCount((int) customers.stream()
                .filter(customer -> customer.getStatus() == CustStatus.PendingMeasure
                        && customer.getMeasureAt() != null
                        && customer.getDesigner() == null)
                .count());
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
        report.setSignedAmount(projects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setConversionRate(rate(report.getAcceptedQuoteCount(), report.getQuoteCount()));

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
                if (task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled) {
                    overdue++;
                }
            }
            reportCount += projectReportRepository.countByProject_ProjectId(project.getProjectId());
        }
        report.setTaskTodoCount(todo);
        report.setTaskOverdueCount(overdue);
        report.setReportCount(reportCount);
        report.setNoticeUnreadCount(noticeRepository.countByReceiver_EmpIdAndReadFalse(current.getEmpId()));

        List<DesignerPerformance> performances = members.stream()
                .filter(member -> member.getRole() == EmployeeRole.Designer)
                .map(designer -> performance(designer, customers, quotes, projects))
                .sorted(Comparator.comparing(DesignerPerformance::getTaskOverdueCount).reversed()
                        .thenComparing(DesignerPerformance::getSignedAmount, Comparator.reverseOrder()))
                .toList();
        report.setDesignerPerformances(performances);
        report.setRecentCustomers(customers.stream().limit(8).toList());
        report.setRecentQuotes(quotes.stream().limit(8).toList());
        report.setRecentProjects(projects.stream().limit(8).toList());
        return report;
    }

    private DesignerPerformance performance(Employee designer,
                                            List<Customer> customers,
                                            List<Quote> quotes,
                                            List<Project> projects) {
        Long designerId = designer.getEmpId();
        List<Customer> ownedCustomers = customers.stream()
                .filter(customer -> customer.getDesigner() != null
                        && designerId.equals(customer.getDesigner().getEmpId()))
                .toList();
        List<Quote> designerQuotes = quotes.stream()
                .filter(quote -> quote.getCreatedBy() != null
                        && designerId.equals(quote.getCreatedBy().getEmpId()))
                .toList();
        List<Project> designerProjects = projects.stream()
                .filter(project -> project.getDesigner() != null
                        && designerId.equals(project.getDesigner().getEmpId()))
                .toList();

        DesignerPerformance performance = new DesignerPerformance();
        performance.setDesigner(designer);
        performance.setCustomerCount(ownedCustomers.size());
        performance.setPendingMeasureCount((int) ownedCustomers.stream()
                .filter(customer -> customer.getStatus() == CustStatus.PendingMeasure)
                .count());
        performance.setQuoteCount(designerQuotes.size());
        performance.setPendingQuoteCount((int) designerQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Draft
                        || quote.getStatus() == QuoteStatus.Submitted)
                .count());
        performance.setAcceptedQuoteCount((int) designerQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .count());
        performance.setQuoteAmount(designerQuotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Rejected && quote.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setSignedAmount(designerProjects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setProjectCount(designerProjects.size());
        performance.setOngoingProjectCount((int) designerProjects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Preparing
                        || project.getStatus() == ProjectStatus.Ongoing
                        || project.getStatus() == ProjectStatus.Accepting)
                .count());
        performance.setConversionRate(rate(performance.getAcceptedQuoteCount(), performance.getQuoteCount()));

        int todo = 0;
        int overdue = 0;
        int reports = 0;
        for (Project project : designerProjects) {
            for (ProjectTask task : taskRepository.findByProject(project)) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) {
                    todo++;
                }
                if (task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled) {
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

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
