package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.PerformanceReportContext;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.PerformanceQuery;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.decoboon.statistic.context.StatisticStoreAggregateContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerformanceReportContextTest {

    @Test
    void shouldCalculateDesignerMarketerAndPmRates() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        AcceptanceRepository acceptanceRepository = mock(AcceptanceRepository.class);
        TenantContext tenant = new TenantContext(employeeRepository);
        PerformanceReportContext context = new PerformanceReportContext(tenant, employeeRepository,
                quoteRepository, customerRepository, projectRepository, acceptanceRepository);
        // DEC-067 范围标注（P1-a2 补）：直接 new 的上下文不经过 Spring 注入，补 mock 依赖。
        StatisticStoreAggregateContext aggregate = mock(StatisticStoreAggregateContext.class);
        when(aggregate.unassignedEmployees(any())).thenReturn(0L);
        ReflectionTestUtils.setField(context, "aggregateContext", aggregate);
        ReflectionTestUtils.setField(context, "storeRepository", mock(StoreRepository.class));

        LocalDate month = LocalDate.of(2026, 9, 1);
        LocalDate day = month.plusDays(9);
        Company company = new Company();
        company.setComId(1L);
        Employee boss = employee(10L, EmployeeRole.Boss, company, "老板");
        Employee designer = employee(11L, EmployeeRole.Designer, company, "设计师");
        Employee marketer = employee(12L, EmployeeRole.Marketer, company, "顾问");
        Employee pm = employee(13L, EmployeeRole.Pm, company, "项目经理");

        Quote quote = new Quote();
        quote.setCreatedBy(designer);
        quote.setStatus(QuoteStatus.Accepted);
        quote.setCreateTime(new DateTime(day.atTime(10, 0)));

        Customer customer = new Customer();
        customer.setOwner(marketer);
        customer.setStatus(CustStatus.Signed);
        customer.setCreateTime(new DateTime(day.atTime(11, 0)));

        Project project = new Project();
        project.setProjectId(20L);
        project.setManager(pm);
        project.setStatus(ProjectStatus.Finished);
        project.setPlanEndDate(day.plusDays(2));
        project.setActualEndDate(day.plusDays(1));
        Acceptance acceptance = new Acceptance();
        acceptance.setProject(project);
        acceptance.setStatus(AcceptStatus.Confirmed);
        acceptance.setCreateTime(new DateTime(day.atTime(12, 0)));

        when(employeeRepository.findByEmpId(10L)).thenReturn(boss);
        // P0B-02：门店条件在 SQL 侧完成（全部门店 → 不带门店条件），故按新的门店范围查询打桩
        when(employeeRepository.findByCompanyStoreScope(eq(1L), eq(true), eq(0L)))
                .thenReturn(List.of(boss, designer, marketer, pm));
        when(quoteRepository.findOpportunityQuotesByStoreScope(eq(1L), any(), any(), any(), eq(true), eq(0L)))
                .thenReturn(List.of(quote));
        when(customerRepository.findByCompanyStoreScope(eq(1L), eq(true), eq(0L))).thenReturn(List.of(customer));
        when(projectRepository.findByCompanyStoreScope(eq(1L), eq(true), eq(0L))).thenReturn(List.of(project));
        when(acceptanceRepository.findByCompanyAndProjectIds(eq(1L), any())).thenReturn(List.of(acceptance));

        PerformanceQuery query = new PerformanceQuery();
        query.setMonth(month);
        PerformanceReport report = context.report(boss, query);

        EmployeePerformance designerResult = result(report, designer);
        assertThat(designerResult.getDesignerConversionRate()).isEqualByComparingTo("100.0");
        assertThat(result(report, marketer).getMarketerSignRate()).isEqualByComparingTo("100.0");
        assertThat(result(report, pm).getPmOnTimeDeliveryRate()).isEqualByComparingTo("100.0");
        assertThat(result(report, pm).getPmAcceptancePassRate()).isEqualByComparingTo("100.0");
        assertThat(report.getDesignerCount()).isEqualTo(1);
        assertThat(report.getMarketerCount()).isEqualTo(1);
        assertThat(report.getPmCount()).isEqualTo(1);
    }

    private EmployeePerformance result(PerformanceReport report, Employee employee) {
        return report.getPerformances().stream()
                .filter(item -> item.getEmployee().getEmpId().equals(employee.getEmpId()))
                .findFirst().orElseThrow();
    }

    private Employee employee(Long id, EmployeeRole role, Company company, String name) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setName(name);
        return employee;
    }
}
