package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.quote.context.QuoteBusinessStateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.service.quotes.designerpage.support.Page;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-040 / BR-033：店长可读本店待签约报价队列（Store 范围），其他岗位仍按原口径拒绝。 */
class DesignerQuoteStoreManagerScopeTest {

    @Test
    void storeManagerShouldReadQuotesOfOwnStore() {
        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        QuoteBusinessStateContext businessState = mock(QuoteBusinessStateContext.class);

        Company company = new Company();
        company.setComId(11L);
        Store store = new Store();
        store.setStoreId(22L);
        Employee manager = new Employee();
        manager.setEmpId(33L);
        manager.setRole(EmployeeRole.StoreManager);
        manager.setCompany(company);
        manager.setStore(store);

        Quote quote = new Quote();
        quote.setQuoteId(44L);

        when(tenantContext.requireEmployee(manager)).thenReturn(manager);
        when(quotes.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(11L, 22L))
                .thenReturn(List.of(quote));

        Page provider = new Page(quotes, tenantContext, businessState);
        ReflectionTestUtils.setField(provider, "employee", manager);

        var result = provider.call(new Quote());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(quote);
    }

    @Test
    void storeManagerWithoutStoreSeesEmptyQueueInsteadOfWholeCompany() {
        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        QuoteBusinessStateContext businessState = mock(QuoteBusinessStateContext.class);

        Company company = new Company();
        company.setComId(11L);
        Employee manager = new Employee();
        manager.setEmpId(33L);
        manager.setRole(EmployeeRole.StoreManager);
        manager.setCompany(company);

        when(tenantContext.requireEmployee(manager)).thenReturn(manager);
        when(quotes.findByCompany_ComIdOrderByQuoteIdDesc(11L)).thenReturn(List.of(new Quote()));

        Page provider = new Page(quotes, tenantContext, businessState);
        ReflectionTestUtils.setField(provider, "employee", manager);

        assertThat(provider.call(new Quote()).getTotalElements()).isZero();
    }

    @Test
    void designerStillReadsOnlyOwnQuotes() {
        QuoteRepository quotes = mock(QuoteRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        QuoteBusinessStateContext businessState = mock(QuoteBusinessStateContext.class);

        Company company = new Company();
        company.setComId(11L);
        Employee designer = new Employee();
        designer.setEmpId(55L);
        designer.setRole(EmployeeRole.Designer);
        designer.setCompany(company);

        Quote own = new Quote();
        own.setQuoteId(66L);

        when(tenantContext.requireEmployee(designer)).thenReturn(designer);
        when(quotes.findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(11L, 55L))
                .thenReturn(List.of(own));

        Page provider = new Page(quotes, tenantContext, businessState);
        ReflectionTestUtils.setField(provider, "employee", designer);

        assertThat(provider.call(new Quote()).getContent()).containsExactly(own);
    }

    @Test
    void otherRolesAreStillRejected() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Marketer, EmployeeRole.Pm, EmployeeRole.Finance, EmployeeRole.TeamLeader}) {
            QuoteRepository quotes = mock(QuoteRepository.class);
            TenantContext tenantContext = mock(TenantContext.class);
            QuoteBusinessStateContext businessState = mock(QuoteBusinessStateContext.class);

            Company company = new Company();
            company.setComId(11L);
            Employee employee = new Employee();
            employee.setEmpId((long) role.ordinal() + 100);
            employee.setRole(role);
            employee.setCompany(company);

            when(tenantContext.requireEmployee(employee)).thenReturn(employee);

            Page provider = new Page(quotes, tenantContext, businessState);
            ReflectionTestUtils.setField(provider, "employee", employee);

            assertThatThrownBy(() -> provider.call(new Quote()))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("仅设计师、店长或企业负责人");
        }
    }
}
