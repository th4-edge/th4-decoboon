package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.context.CustomerInsightContext;
import com.th4.edge.decoboon.customer.context.CustomerOverviewContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOverviewDormantTest {

    @Test
    void dormantCustomerShouldNotAppearAsRiskAndShouldHaveOwnStageBucket() {
        TenantContext tenant = mock(TenantContext.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerFollowRepository follows = mock(CustomerFollowRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        MeasureRepository measures = mock(MeasureRepository.class);
        CustomerOverviewContext context = new CustomerOverviewContext(
                tenant, customers, follows, quotes, contracts, measures, new CustomerInsightContext());

        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenant.requireBoss(boss)).thenReturn(boss);

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
