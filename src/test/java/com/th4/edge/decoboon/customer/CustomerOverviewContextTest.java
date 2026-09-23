package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.context.CustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOverviewContextTest {

    private CustomerOverviewContext context;
    private Employee boss;
    private List<Customer> customers;
    private List<Measure> measures;

    @BeforeEach
    void setUp() {
        TenantContext tenant = mock(TenantContext.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerFollowRepository followRepository = mock(CustomerFollowRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        context = new CustomerOverviewContext(tenant, customerRepository, followRepository, quoteRepository,
                contractRepository, measureRepository);

        Company company = new Company();
        company.setComId(10L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenant.requireBoss(boss)).thenReturn(boss);

        LocalDate today = LocalDate.now();
        Customer overdue = customer(101L, CustStatus.Following, "逾期客户", company, today.minusDays(20).atStartOfDay());
        overdue.setNextFollowAt(today.minusDays(2).atTime(9, 0));
        overdue.setOwner(boss);
        Customer fresh = customer(102L, CustStatus.New, "新客户", company, today.atTime(8, 0));
        fresh.setOwner(boss);
        Customer quoted = customer(103L, CustStatus.Quoted, "报价客户", company, today.minusDays(3).atStartOfDay());
        quoted.setOwner(boss);
        quoted.setLastFollowAt(today.minusDays(1).atTime(10, 0));
        quoted.setNextFollowAt(today.plusDays(2).atTime(10, 0));
        customers = List.of(overdue, fresh, quoted);

        CustomerFollow follow = new CustomerFollow();
        follow.setFollowId(201L);
        follow.setCompany(company);
        follow.setCustomer(quoted);
        follow.setEmp(boss);
        follow.setCreateTime(new DateTime(today.minusDays(1).atTime(10, 0)));

        Quote quote = new Quote();
        quote.setQuoteId(301L);
        quote.setCustomer(quoted);
        quote.setAmount(new BigDecimal("50000"));

        Project project = new Project();
        project.setProjectId(401L);
        project.setName("签约项目");
        ProjectContract contract = new ProjectContract();
        contract.setContractId(501L);
        contract.setCompany(company);
        contract.setCustomer(quoted);
        contract.setProject(project);
        contract.setSignedAt(today);
        contract.setSignedAmount(new BigDecimal("100000"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayTime = now.isAfter(todayStart.plusSeconds(1)) ? todayStart.plusSeconds(1) : now;
        LocalDateTime yesterdayTime = today.minusDays(1).atTime(10, 0);
        LocalDateTime last7Time = today.minusDays(3).atTime(10, 0);
        measures = List.of(
                measure(601L, overdue, MeasureStatus.Cancelled, todayTime.minusMinutes(1), null, null, todayTime.minusMinutes(1)),
                measure(602L, overdue, MeasureStatus.Scheduled, last7Time, null, boss, last7Time),
                measure(603L, fresh, MeasureStatus.Scheduled, null, null, null, todayTime),
                measure(604L, quoted, MeasureStatus.Completed, todayTime, todayTime, boss, todayTime),
                measure(605L, quoted, MeasureStatus.Scheduled, todayTime, null, boss, todayTime),
                measure(606L, quoted, MeasureStatus.Scheduled, yesterdayTime, null, boss, yesterdayTime)
        );

        when(customerRepository.findByCompany_ComId(10L)).thenReturn(customers);
        when(followRepository.findByCompany_ComId(10L)).thenReturn(List.of(follow));
        when(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(10L)).thenReturn(List.of(quote));
        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(10L)).thenReturn(List.of(contract));
        when(measureRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(
                10L, List.of(101L, 102L, 103L))).thenReturn(measures);
    }

    @Test
    void shouldBuildMetricsStageCountsAndMeasureCustomerWindows() {
        CustomerOverviewReport report = context.report(boss, 1, 20, null, "all", "risk", null);

        assertThat(report.getMetrics()).hasSize(4);
        assertThat(report.getMetrics()).extracting(CustomerMetricCard::getKey)
                .containsExactly("newCustomer", "activeFollow", "signedCustomer", "measureCustomer");
        CustomerMetricCard signed = report.getMetrics().stream()
                .filter(item -> item.getKey().equals("signedCustomer")).findFirst().orElseThrow();
        assertThat(signed.getValue()).isEqualByComparingTo("1");

        CustomerMetricCard measureCustomer = report.getMetrics().stream()
                .filter(item -> item.getKey().equals("measureCustomer")).findFirst().orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> expectedMonthCustomers = measures.stream()
                .filter(item -> item.getStatus() != MeasureStatus.Cancelled)
                .filter(item -> effectiveScheduledAt(item) != null
                        && !effectiveScheduledAt(item).isBefore(monthStart)
                        && effectiveScheduledAt(item).isBefore(now))
                .map(item -> item.getCustomer().getCustId())
                .collect(Collectors.toSet());
        assertThat(measureCustomer.getLabel()).isEqualTo("预约量房客户");
        assertThat(measureCustomer.getValue()).isEqualByComparingTo(String.valueOf(expectedMonthCustomers.size()));
        assertThat(measureCustomer.getSubText()).isEqualTo("本月完成 1 户 · 待派单 1 户");
        assertThat(periodValue(measureCustomer, "today")).isEqualByComparingTo("2");
        assertThat(periodValue(measureCustomer, "yesterday")).isEqualByComparingTo("1");
        assertThat(periodValue(measureCustomer, "last7Days")).isEqualByComparingTo("3");
        assertThat(periodValue(measureCustomer, "month"))
                .isEqualByComparingTo(String.valueOf(expectedMonthCustomers.size()));
        assertThat(expectedMonthCustomers).contains(102L, 103L);
        assertThat(report.getStageCounts()).anySatisfy(item -> {
            if (item.getKey().equals("quote")) assertThat(item.getCount()).isEqualTo(1);
        });
        assertThat(report.getCustomers()).hasSize(3);
        assertThat(report.getCustomers().get(0).getName()).isEqualTo("逾期客户");
        assertThat(report.getCustomers().get(0).getRiskLevel()).isEqualTo(CustomerRiskLevel.High);
        assertThat(report.getCustomers().get(0).getNextAction()).contains("逾期跟进");
        assertThat(report.getCustomers().stream().filter(item -> item.getCustId().equals(103L))
                .findFirst().orElseThrow().getSignedAmount()).isEqualByComparingTo("100000");
        assertThat(report.getOwnerGroups()).anySatisfy(group -> {
            if (Long.valueOf(1L).equals(group.getOwnerId())) {
                assertThat(group.getRiskCustomerCount()).isEqualTo(1);
            }
        });
    }

    private java.math.BigDecimal periodValue(CustomerMetricCard card, String key) {
        return card.getPeriods().stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElseThrow()
                .getValue();
    }

    private Measure measure(Long id, Customer customer, MeasureStatus status, LocalDateTime scheduledAt,
                            LocalDateTime completedAt, Employee designer, LocalDateTime measureTime) {
        Measure measure = new Measure();
        measure.setMeasureId(id);
        measure.setCustomer(customer);
        measure.setStatus(status);
        measure.setScheduledAt(scheduledAt);
        measure.setCompletedAt(completedAt);
        measure.setDesigner(designer);
        measure.setMeasureTime(measureTime);
        return measure;
    }

    private LocalDateTime effectiveScheduledAt(Measure measure) {
        return measure.getScheduledAt() != null ? measure.getScheduledAt() : measure.getMeasureTime();
    }

    private Customer customer(Long id, CustStatus status, String name, Company company, LocalDateTime created) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(company);
        customer.setStatus(status);
        customer.setName(name);
        customer.setTelNo("13900000000");
        customer.setCreateTime(new DateTime(created));
        return customer;
    }
}
