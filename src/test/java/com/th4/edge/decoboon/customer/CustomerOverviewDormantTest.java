package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.context.CustomerInsightContext;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
import com.th4.edge.decoboon.customer.context.CustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOverviewDormantTest {

    @Test
    void dormantCustomerShouldNotAppearAsRiskAndShouldHaveOwnStageBucket() {
        TenantContext tenant = mock(TenantContext.class);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerFollowRepository follows = mock(CustomerFollowRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        CustomerOverviewContext context = new CustomerOverviewContext(
                tenant, dataScope, customers, quotes, contracts,
                new CustomerInsightContext(), mock(CustomerLossContext.class));

        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenant.requireEmployee(boss)).thenReturn(boss);
        when(dataScope.requireScope(boss)).thenReturn(new DataScope(DataScopeType.Company, 10L, null, null, 1L));

        Customer dormant = new Customer();
        dormant.setCustId(101L);
        dormant.setCompany(company);
        dormant.setName("休眠客户");
        dormant.setTelNo("13900000000");
        dormant.setStatus(CustStatus.Dormant);
        dormant.setNextFollowAt(LocalDateTime.now().minusDays(10));
        dormant.setIdleDays(30);
        when(customers.findByCompany_ComId(10L)).thenReturn(List.of(dormant));
        when(follows.findByCompany_ComId(10L)).thenReturn(List.of());
        when(quotes.findByCompany_ComIdOrderByQuoteIdDesc(10L)).thenReturn(List.of());
        when(contracts.findByCompany_ComIdOrderByContractIdDesc(10L)).thenReturn(List.of());
        when(measures.findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(10L, List.of(101L)))
                .thenReturn(List.of());
        when(customers.countOwnerGroupsByStoreScope(10L, true, null))
                .thenReturn(List.<Object[]>of(new Object[]{null, null, 1L, 0L, 0L}));
        when(customers.countStatusByStoreScope(10L, true, null))
                .thenReturn(List.<Object[]>of(new Object[]{CustStatus.Dormant, 1L}));
        when(customers.countFollowDueByStoreScope(any(), anyBoolean(), any(), any())).thenReturn(0L);
        when(customers.countPendingAssignByStoreScope(10L, true, null)).thenReturn(0L);
        when(customers.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(dormant), invocation.getArgument(1), 1));
        when(quotes.findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(10L, List.of(101L)))
                .thenReturn(List.of());
        when(contracts.findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(10L, List.of(101L)))
                .thenReturn(List.of());

        CustomerOverviewReport report = context.report(boss, 1, 20, null, "all", "risk", null);

        Customer result = report.getCustomers().get(0);
        assertThat(result.getRiskLevel()).isEqualTo(CustomerRiskLevel.Normal);
        assertThat(result.getRiskReason()).contains("休眠");
        assertThat(result.getNextAction()).isEqualTo("恢复跟进");
        assertThat(report.getStageCounts())
                .anySatisfy(item -> {
                    if (item.getKey().equals("dormant")) assertThat(item.getCount()).isEqualTo(1);
                });
    }
}
