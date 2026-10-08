package com.th4.edge.decoboon.statistic.service.marketer.report.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.model.MarketerPeriodMetric;
import com.th4.edge.decoboon.statistic.model.MarketerRankingItem;
import com.th4.edge.decoboon.statistic.model.MarketerRateMetric;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final PropertyRepository propertyRepository;
    private final CustomerFollowRepository followRepository;
    private final EmployeeRepository employeeRepository;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  QuoteRepository quoteRepository,
                  PropertyRepository propertyRepository,
                  CustomerFollowRepository followRepository,
                  EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.quoteRepository = quoteRepository;
        this.propertyRepository = propertyRepository;
        this.followRepository = followRepository;
        this.employeeRepository = employeeRepository;
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
        List<Property> properties = customerIds.isEmpty()
                ? List.of()
                : propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds);
        List<CustomerFollow> follows = followRepository.findByCompany_ComIdAndEmp_EmpId(comId, empId);
        List<Customer> companyCustomers = customerRepository.findByCompany_ComId(comId);
        List<Long> companyCustomerIds = companyCustomers.stream().map(Customer::getCustId).toList();
        List<Property> companyProperties = companyCustomerIds.isEmpty()
                ? List.of()
                : propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, companyCustomerIds);
        List<CustomerFollow> companyFollows = followRepository.findByCompany_ComId(comId);
        List<Employee> marketers = employeeRepository.findByCompany_ComIdAndRoleOrderByName(comId, EmployeeRole.Marketer);
        Map<Long, List<Property>> propertiesByCustomer = groupProperties(companyProperties);
        Map<Long, List<CustomerFollow>> followsByEmployee = groupFollows(companyFollows);

        MarketerReport report = new MarketerReport();
        report.setCustomerCount(customers.size());
        report.setDormantCustomerCount(count(customers, CustStatus.Dormant));
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
            if (customer.getStatus() == CustStatus.Lost
                    || customer.getStatus() == CustStatus.Dormant
                    || customer.getCreateTime() == null) {
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
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        report.setCustomerInputDay(periodMetric(
                customersCreated(customers, today, today.plusDays(1)),
                customersCreated(customers, today.minusDays(1), today)));
        report.setCustomerInputWeek(periodMetric(
                customersCreated(customers, weekStart, weekStart.plusWeeks(1)),
                customersCreated(customers, weekStart.minusWeeks(1), weekStart)));
        report.setCustomerInputMonth(periodMetric(
                customersCreated(customers, monthStart, monthStart.plusMonths(1)),
                customersCreated(customers, monthStart.minusMonths(1), monthStart)));

        report.setFollowDay(periodMetric(
                followsCreated(follows, today, today.plusDays(1)),
                followsCreated(follows, today.minusDays(1), today)));
        report.setFollowWeek(periodMetric(
                followsCreated(follows, weekStart, weekStart.plusWeeks(1)),
                followsCreated(follows, weekStart.minusWeeks(1), weekStart)));
        report.setFollowMonth(periodMetric(
                followsCreated(follows, monthStart, monthStart.plusMonths(1)),
                followsCreated(follows, monthStart.minusMonths(1), monthStart)));

        report.setMeasureRateDay(rateMetric(
                measureRate(customers, propertiesByCustomer, today, today.plusDays(1)),
                measureRate(customers, propertiesByCustomer, today.minusDays(1), today)));
        report.setMeasureRateWeek(rateMetric(
                measureRate(customers, propertiesByCustomer, weekStart, weekStart.plusWeeks(1)),
                measureRate(customers, propertiesByCustomer, weekStart.minusWeeks(1), weekStart)));
        report.setMeasureRateMonth(rateMetric(
                measureRate(customers, propertiesByCustomer, monthStart, monthStart.plusMonths(1)),
                measureRate(customers, propertiesByCustomer, monthStart.minusMonths(1), monthStart)));

        List<MarketerRankingItem> marketerRankings = buildMarketerRankings(
                marketers, companyCustomers, propertiesByCustomer, followsByEmployee, monthStart, monthStart.plusMonths(1));
        List<MarketerRankingItem> storeRankings = buildStoreRankings(marketers, marketerRankings);
        report.setMarketerRankings(marketerRankings);
        report.setStoreRankings(storeRankings);
        report.setCompanyMarketerCount(marketers.size());
        report.setCompanyStoreCount(storeRankings.size());
        report.setMyMarketerRank(marketerRankings.stream()
                .filter(item -> item.getId().equals(empId)).map(MarketerRankingItem::getRank).findFirst().orElse(0));
        Long currentStoreId = current.getStore() == null ? null : current.getStore().getStoreId();
        report.setMyStoreRank(currentStoreId == null ? 0 : storeRankings.stream()
                .filter(item -> item.getId().equals(currentStoreId)).map(MarketerRankingItem::getRank).findFirst().orElse(0));
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
        // DEC-036：任务改为循环外一次批量取数（原为每项目 1 条）
        List<Long> projectIds = projects.stream().map(Project::getProjectId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<Long, List<ProjectTask>> tasksByProject = projectIds.isEmpty() ? java.util.Map.of()
                : taskRepository.findByProject_ProjectIdIn(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.getProject().getProjectId()));
        for (Project project : projects) {
            for (ProjectTask task : tasksByProject.getOrDefault(project.getProjectId(), List.of())) {
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

    private Map<Long, List<Property>> groupProperties(List<Property> properties) {
        Map<Long, List<Property>> grouped = new HashMap<>();
        for (Property property : properties) {
            if (property.getCustomer() == null) continue;
            grouped.computeIfAbsent(property.getCustomer().getCustId(), ignored -> new ArrayList<>()).add(property);
        }
        return grouped;
    }

    private Map<Long, List<CustomerFollow>> groupFollows(List<CustomerFollow> follows) {
        Map<Long, List<CustomerFollow>> grouped = new HashMap<>();
        for (CustomerFollow follow : follows) {
            if (follow.getEmp() == null) continue;
            grouped.computeIfAbsent(follow.getEmp().getEmpId(), ignored -> new ArrayList<>()).add(follow);
        }
        return grouped;
    }

    private int customersCreated(List<Customer> customers, LocalDate start, LocalDate end) {
        if (customers == null || customers.isEmpty()) {
            return 0;
        }
        return (int) customers.stream().filter(customer -> {
            if (customer.getCreateTime() == null) return false;
            LocalDate date = customer.getCreateTime().toLocalDate();
            return !date.isBefore(start) && date.isBefore(end);
        }).count();
    }

    private int followsCreated(List<CustomerFollow> follows, LocalDate start, LocalDate end) {
        if (follows == null || follows.isEmpty()) {
            return 0;
        }
        return (int) follows.stream().filter(follow -> {
            if (follow.getCreateTime() == null) return false;
            LocalDate date = follow.getCreateTime().toLocalDate();
            return !date.isBefore(start) && date.isBefore(end);
        }).count();
    }

    private MarketerPeriodMetric periodMetric(int current, int previous) {
        return new MarketerPeriodMetric(
                BigDecimal.valueOf(current),
                BigDecimal.valueOf(previous),
                growthRate(BigDecimal.valueOf(current), BigDecimal.valueOf(previous)));
    }

    private MarketerRateMetric rateMetric(BigDecimal current, BigDecimal previous) {
        return new MarketerRateMetric(current, previous, current.subtract(previous).setScale(1, RoundingMode.HALF_UP));
    }

    private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal measureRate(List<Customer> customers,
                                   Map<Long, List<Property>> propertiesByCustomer,
                                   LocalDate start,
                                   LocalDate end) {
        List<Customer> cohort = customers.stream().filter(customer -> {
            if (customer.getCreateTime() == null) return false;
            LocalDate date = customer.getCreateTime().toLocalDate();
            return !date.isBefore(start) && date.isBefore(end);
        }).toList();
        if (cohort.isEmpty()) return BigDecimal.ZERO;
        long measured = cohort.stream()
                .filter(customer -> hasMeasuredProperty(propertiesByCustomer.get(customer.getCustId())))
                .count();
        return BigDecimal.valueOf(measured * 100.0 / cohort.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean hasMeasuredProperty(List<Property> properties) {
        if (properties == null || properties.isEmpty()) return false;
        return properties.stream().anyMatch(property -> switch (property.getStatus()) {
            case Measured, Designing, Quoted, Contracted, Serving, Completed -> true;
            default -> false;
        });
    }

    private List<MarketerRankingItem> buildMarketerRankings(List<Employee> marketers,
                                                            List<Customer> customers,
                                                            Map<Long, List<Property>> propertiesByCustomer,
                                                            Map<Long, List<CustomerFollow>> followsByEmployee,
                                                            LocalDate start,
                                                            LocalDate end) {
        List<MarketerRankingItem> items = new ArrayList<>();
        for (Employee marketer : marketers) {
            List<Customer> owned = customers.stream()
                    .filter(customer -> customer.getOwner() != null
                            && customer.getOwner().getEmpId().equals(marketer.getEmpId()))
                    .toList();
            int customerCount = customersCreated(owned, start, end);
            int measureCount = (int) owned.stream().filter(customer -> {
                if (customer.getCreateTime() == null) return false;
                LocalDate date = customer.getCreateTime().toLocalDate();
                return !date.isBefore(start) && date.isBefore(end)
                        && hasMeasuredProperty(propertiesByCustomer.get(customer.getCustId()));
            }).count();
            int followCount = followsCreated(followsByEmployee.get(marketer.getEmpId()), start, end);
            MarketerRankingItem item = new MarketerRankingItem();
            item.setId(marketer.getEmpId());
            item.setName(marketer.getName());
            item.setCustomerCount(customerCount);
            item.setMeasureCount(measureCount);
            item.setFollowCount(followCount);
            item.setMeasureRate(customerCount == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(measureCount * 100.0 / customerCount).setScale(1, RoundingMode.HALF_UP));
            items.add(item);
        }
        items.sort(rankingComparator());
        for (int i = 0; i < items.size(); i++) items.get(i).setRank(i + 1);
        return items;
    }

    private List<MarketerRankingItem> buildStoreRankings(List<Employee> marketers,
                                                          List<MarketerRankingItem> marketerRankings) {
        Map<Long, MarketerRankingItem> byEmployee = new HashMap<>();
        marketerRankings.forEach(item -> byEmployee.put(item.getId(), item));
        Map<Long, List<MarketerRankingItem>> byStore = new LinkedHashMap<>();
        Map<Long, String> storeNames = new HashMap<>();
        for (Employee marketer : marketers) {
            if (marketer.getStore() == null || marketer.getStore().getStoreId() == null) continue;
            Long storeId = marketer.getStore().getStoreId();
            storeNames.put(storeId, marketer.getStore().getName());
            byStore.computeIfAbsent(storeId, ignored -> new ArrayList<>()).add(byEmployee.get(marketer.getEmpId()));
        }
        List<MarketerRankingItem> items = new ArrayList<>();
        byStore.forEach((storeId, people) -> {
            int customerCount = people.stream().mapToInt(MarketerRankingItem::getCustomerCount).sum();
            int measureCount = people.stream().mapToInt(MarketerRankingItem::getMeasureCount).sum();
            int followCount = people.stream().mapToInt(MarketerRankingItem::getFollowCount).sum();
            MarketerRankingItem item = new MarketerRankingItem();
            item.setId(storeId);
            item.setName(storeNames.get(storeId));
            item.setCustomerCount(customerCount);
            item.setMeasureCount(measureCount);
            item.setFollowCount(followCount);
            item.setMeasureRate(customerCount == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(measureCount * 100.0 / customerCount).setScale(1, RoundingMode.HALF_UP));
            items.add(item);
        });
        items.sort(rankingComparator());
        for (int i = 0; i < items.size(); i++) items.get(i).setRank(i + 1);
        return items;
    }

    private Comparator<MarketerRankingItem> rankingComparator() {
        return Comparator.comparing(MarketerRankingItem::getCustomerCount, Comparator.reverseOrder())
                .thenComparing(MarketerRankingItem::getMeasureRate, Comparator.reverseOrder())
                .thenComparing(MarketerRankingItem::getFollowCount, Comparator.reverseOrder())
                .thenComparing(MarketerRankingItem::getName, Comparator.nullsLast(String::compareTo));
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
