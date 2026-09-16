package com.th4.edge.decoboon.statistic.service.marketer.report.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.model.MarketerReport;
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
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Note("业务员经营报告")
@Description("聚合当前业务员客户、项目、任务和业绩指标")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr",
                        "area", "budget", "status", "owner", "wechatAdded", "wechatAddedAt", "visitCount", "lastVisitAt", "lastFollowAt",
                        "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "status", "progress",
                        "siteAddr", "area", "contractAmount", "startDate", "planEndDate" }),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("statistic/marketer/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.marketer.report.Report {

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final QuoteRepository quoteRepository;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  QuoteRepository quoteRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.quoteRepository = quoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MarketerReport call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        if (current.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        List<Customer> customers = customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId);
        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        List<Project> projects = customerIds.isEmpty()
                ? List.of()
                : projectRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds);
        List<Quote> quotes = customerIds.isEmpty()
                ? List.of()
                : quoteRepository.findByCustomer_CustIdIn(customerIds);

        MarketerReport report = new MarketerReport();
        report.setCustomerCount(customers.size());
        report.setNewCustomerCount(count(customers, CustStatus.New));
        report.setFollowingCustomerCount(count(customers, CustStatus.Following, CustStatus.Quoted));
        report.setSignedCustomerCount(count(customers, CustStatus.Signed, CustStatus.Serving, CustStatus.Finished));
        report.setPendingMeasureCount(count(customers, CustStatus.PendingMeasure));
        report.setQuotedCustomerCount(count(customers, CustStatus.Quoted));
        report.setDraftQuoteCount((int) quotes.stream().filter(q -> q.getStatus() == QuoteStatus.Draft).count());
        report.setSubmittedQuoteCount((int) quotes.stream().filter(q -> q.getStatus() == QuoteStatus.Submitted).count());
        report.setLostCustomerCount(count(customers, CustStatus.Lost));
        report.setProjectCount(projects.size());
        report.setOngoingProjectCount(projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.Preparing
                        || p.getStatus() == ProjectStatus.Ongoing
                        || p.getStatus() == ProjectStatus.Accepting)
                .toList().size());
        report.setFinishedProjectCount(projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.Finished || p.getStatus() == ProjectStatus.Settled)
                .toList().size());
        report.setQuoteAmount(quotes.stream()
                .filter(q -> q.getStatus() != QuoteStatus.Rejected && q.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setAcceptedQuoteAmount(quotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.Accepted || q.getStatus() == QuoteStatus.Converted)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setSignedAmount(projects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setWechatAddedCount((int) customers.stream()
                .filter(c -> Boolean.TRUE.equals(c.getWechatAdded()))
                .count());
        report.setConvertedCustomerCount(count(customers,
                CustStatus.PendingMeasure, CustStatus.Quoted, CustStatus.Signed,
                CustStatus.Serving, CustStatus.Finished));
        report.setConversionRate(customers.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(report.getSignedCustomerCount() * 100.0 / customers.size())
                .setScale(1, RoundingMode.HALF_UP));

        List<Project> signedProjects = projects.stream()
                .filter(p -> p.getCustomer() != null && p.getCreateTime() != null
                        && p.getCustomer().getCreateTime() != null
                        && (p.getCustomer().getStatus() == CustStatus.Signed
                        || p.getCustomer().getStatus() == CustStatus.Serving
                        || p.getCustomer().getStatus() == CustStatus.Finished))
                .toList();
        report.setAverageConversionDays(signedProjects.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(signedProjects.stream().mapToLong(p -> Math.max(0,
                        ChronoUnit.HOURS.between(
                                p.getCustomer().getCreateTime().toLocalDateTime(),
                                p.getCreateTime().toLocalDateTime())))
                        .average().orElse(0) / 24.0)
                .setScale(1, RoundingMode.HALF_UP));

        LocalDateTime now = LocalDateTime.now();
        int hot = 0;
        int warm = 0;
        int cooling = 0;
        int risk = 0;
        int followDue = 0;
        int followOverdue = 0;
        LocalDate today = LocalDate.now();
        for (Customer customer : customers) {
            if (customer.getStatus() == CustStatus.Lost || customer.getCreateTime() == null) {
                continue;
            }
            if (customer.getStatus() != CustStatus.Signed
                    && customer.getStatus() != CustStatus.Serving
                    && customer.getStatus() != CustStatus.Finished
                    && customer.getNextFollowAt() != null) {
                LocalDate nextDate = customer.getNextFollowAt().toLocalDate();
                if (nextDate.isBefore(today)) followOverdue++;
                else if (nextDate.isEqual(today)) followDue++;
            }
            long days = Math.max(0, ChronoUnit.DAYS.between(
                    customer.getCreateTime().toLocalDateTime(), now));
            if (days <= 3) hot++;
            else if (days <= 7) warm++;
            else if (days <= 15) cooling++;
            else risk++;
        }
        report.setFollowDueCount(followDue);
        report.setFollowOverdueCount(followOverdue);
        report.setGoldenHotCount(hot);
        report.setGoldenWarmCount(warm);
        report.setGoldenCoolingCount(cooling);
        report.setGoldenRiskCount(risk);
        report.setRecentCustomers(customers.stream()
                .sorted(Comparator.comparing(Customer::getCustId).reversed())
                .limit(5)
                .toList());
        report.setRecentProjects(projects.stream()
                .sorted(Comparator.comparing(Project::getProjectId).reversed())
                .limit(5)
                .toList());

        int todo = 0;
        int overdue = 0;
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
        }
        report.setTaskTodoCount(todo);
        report.setTaskOverdueCount(overdue);
        return report;
    }

    private int count(List<Customer> customers, CustStatus... statuses) {
        return (int) customers.stream().filter(customer -> {
            for (CustStatus status : statuses) {
                if (customer.getStatus() == status) return true;
            }
            return false;
        }).count();
    }
}
