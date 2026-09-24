package com.th4.edge.decoboon.statistic.service.designer.report.support;

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
import com.th4.edge.decoboon.statistic.model.DesignerReport;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 设计师经营报告实现。 */
@Note("设计师经营报告")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "area", "budget", "status", "owner"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "customer", "createdBy", "area", "amount", "status", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "designer", "siteAddr", "area",
                        "contractAmount", "status", "progress", "planEndDate", "createTime" }),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("statistic/designer/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.designer.report.Report {

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectReportRepository projectReportRepository;
    private final NoticeRepository noticeRepository;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  QuoteRepository quoteRepository,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  ProjectReportRepository projectReportRepository,
                  NoticeRepository noticeRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectReportRepository = projectReportRepository;
        this.noticeRepository = noticeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DesignerReport call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        if (current.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        List<Quote> quotes = quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(comId, empId);
        List<Project> projects = projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);

        List<Long> customerIds = java.util.stream.Stream.of(
                        customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId).stream()
                                .map(Customer::getCustId),
                        quotes.stream().map(q -> q.getCustomer().getCustId()),
                        projects.stream().map(p -> p.getCustomer().getCustId()))
                .flatMap(stream -> stream)
                .distinct().toList();
        List<Customer> customers = customerRepository.findAllById(customerIds);

        DesignerReport report = new DesignerReport();
        report.setCustomerCount(customers.size());
        report.setFollowingCustomerCount(count(customers, CustStatus.New, CustStatus.Following, CustStatus.Quoted));
        report.setSignedCustomerCount(count(customers, CustStatus.Signed, CustStatus.Serving, CustStatus.Finished));
        long effectiveQuoteCount = quotes.stream()
                .filter(q -> q.getStatus() != QuoteStatus.Rejected && q.getStatus() != QuoteStatus.Superseded)
                .count();
        report.setQuoteCount((int) effectiveQuoteCount);
        report.setPendingQuoteCount((int) quotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.Draft || q.getStatus() == QuoteStatus.Submitted).count());
        report.setAcceptedQuoteCount((int) quotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.Accepted || q.getStatus() == QuoteStatus.Converted).count());
        report.setQuoteAmount(quotes.stream().filter(q -> q.getStatus() != QuoteStatus.Rejected && q.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setSignedAmount(projects.stream().map(Project::getContractAmount)
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
        report.setOngoingProjectCount((int) projects.stream().filter(p -> p.getStatus() == ProjectStatus.Preparing
                || p.getStatus() == ProjectStatus.Ongoing || p.getStatus() == ProjectStatus.Accepting).count());
        report.setFinishedProjectCount((int) projects.stream().filter(p -> p.getStatus() == ProjectStatus.Finished
                || p.getStatus() == ProjectStatus.Settled).count());

        int todo = 0;
        int done = 0;
        int overdue = 0;
        int reportCount = 0;
        for (Project project : projects) {
            for (ProjectTask task : taskRepository.findByProject(project)) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) todo++;
                if (task.getStatus() == TaskStatus.Done) done++;
                if (task.getPlannedEnd() != null && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done && task.getStatus() != TaskStatus.Cancelled) overdue++;
            }
            reportCount += projectReportRepository.countByProject_ProjectId(project.getProjectId());
        }
        report.setTaskTodoCount(todo);
        report.setTaskDoneCount(done);
        report.setTaskOverdueCount(overdue);
        report.setReportCount(reportCount);
        report.setNoticeUnreadCount(noticeRepository.countByReceiver_EmpIdAndReadFalse(empId));
        report.setRecentCustomers(customers.stream()
                .sorted(Comparator.comparing(Customer::getCustId).reversed()).limit(5).toList());
        report.setRecentQuotes(quotes.stream().limit(5).toList());
        report.setRecentProjects(projects.stream().limit(5).toList());
        return report;
    }

    private int count(List<Customer> customers, CustStatus... statuses) {
        return (int) customers.stream().filter(customer -> {
            for (CustStatus status : statuses) if (customer.getStatus() == status) return true;
            return false;
        }).count();
    }
}
