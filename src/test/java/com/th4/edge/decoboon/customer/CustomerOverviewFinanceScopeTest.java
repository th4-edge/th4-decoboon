package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.CustStatus;
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
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerOverviewFinanceScopeTest {

    @Test
    void storeFinanceShouldSeeOwnerGroupsOnlyWithinOwnStoreScope() {
        Company company = company(1L);
        Store store = store(7L);
        Employee finance = employee(9L, "门店财务", EmployeeRole.Finance, company, store);
        Employee firstOwner = employee(11L, "业务员甲", EmployeeRole.Marketer, company, store);
        Employee secondOwner = employee(12L, "业务员乙", EmployeeRole.Marketer, company, store);

        Customer first = customer(101L, "客户甲", company, store, firstOwner);
        Customer second = customer(102L, "客户乙", company, store, secondOwner);

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

        when(tenant.requireEmployee(finance)).thenReturn(finance);
        when(dataScope.requireScope(finance)).thenReturn(new DataScope(DataScopeType.Store, 1L, 7L, null, 9L));
        when(customers.findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(1L, 7L))
                .thenReturn(List.of(first, second));
        when(follows.findByCompany_ComId(1L)).thenReturn(List.of());
        when(quotes.findByCompany_ComIdOrderByQuoteIdDesc(1L)).thenReturn(List.of());
        when(contracts.findByCompany_ComIdOrderByContractIdDesc(1L)).thenReturn(List.of());
        when(measures.findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of());
        when(customers.countOwnerGroupsByStoreScope(1L, false, 7L)).thenReturn(List.<Object[]>of(
                new Object[]{11L, "业务员甲", 1L, 0L, 0L},
                new Object[]{12L, "业务员乙", 1L, 0L, 0L}));
        when(customers.countStatusByStoreScope(1L, false, 7L))
                .thenReturn(List.<Object[]>of(new Object[]{CustStatus.Following, 2L}));
        when(customers.countFollowDueByStoreScope(any(), anyBoolean(), any(), any())).thenReturn(0L);
        when(customers.countPendingAssignByStoreScope(1L, false, 7L)).thenReturn(0L);
        when(customers.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(first, second), invocation.getArgument(1), 2));
        when(quotes.findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of());
        when(contracts.findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(1L, List.of(101L, 102L)))
                .thenReturn(List.of());

        CustomerOverviewReport report = context.report(finance, 1, 20, null, "all", "risk", 7L);

        assertThat(report.getCustomers()).extracting(Customer::getName).containsExactlyInAnyOrder("客户甲", "客户乙");
        assertThat(report.getOwnerGroups()).hasSize(2);
        assertThat(report.getOwnerGroups()).extracting(item -> item.getOwnerName())
                .containsExactlyInAnyOrder("业务员甲", "业务员乙");
        verify(customers, never()).findByCompany_ComId(1L);
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Store store(Long id) {
        Store store = new Store();
        store.setStoreId(id);
        return store;
    }

    private Employee employee(Long id, String name, EmployeeRole role, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store);
        return employee;
    }

    private Customer customer(Long id, String name, Company company, Store store, Employee owner) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setName(name);
        customer.setTelNo("13900000000");
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setStatus(CustStatus.Following);
        return customer;
    }
}
