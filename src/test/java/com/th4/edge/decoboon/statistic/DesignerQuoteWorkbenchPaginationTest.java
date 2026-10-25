package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.QuoteWorkbenchItem;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DQ-X1：待谈单队列必须数据库分页、批量组装，并保持角色和数据范围。 */
class DesignerQuoteWorkbenchPaginationTest {

    @Test
    void designerQueueUsesIdPageAndSingleBatchFetch() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        Employee designer = employee(EmployeeRole.Designer, 10L);
        when(employees.findByEmpId(10L)).thenReturn(designer);
        when(quotes.findWorkbenchQuoteIds(
                eq(1L), eq(true), isNull(), eq(10L), isNull(), isNull(), eq(""), eq("%%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(501L, 502L), PageRequest.of(1, 20), 41));
        Quote first = quote(501L, 11L, 101L, designer);
        Quote second = quote(502L, 12L, 102L, designer);
        when(quotes.findWorkbenchQuotes(List.of(501L, 502L))).thenReturn(List.of(second, first));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(
                new TenantContext(employees), quotes);
        ReflectionTestUtils.setField(provider, "current", 2);
        ReflectionTestUtils.setField(provider, "size", 500);

        List<QuoteWorkbenchItem> result = provider.call(designer).getContent();

        assertThat(result).extracting(QuoteWorkbenchItem::getQuoteId).containsExactly(501L, 502L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(quotes).findWorkbenchQuoteIds(
                eq(1L), eq(true), isNull(), eq(10L), isNull(), isNull(), eq(""), eq("%%"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        verify(quotes).findWorkbenchQuotes(List.of(501L, 502L));
        verify(quotes, never()).findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(any(), any(), any());
    }

    @Test
    void storeManagerWithoutStoreCannotSeeWholeCompany() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        Employee manager = employee(EmployeeRole.StoreManager, 20L);
        when(employees.findByEmpId(20L)).thenReturn(manager);

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(
                new TenantContext(employees), quotes);

        assertThat(provider.call(manager).getContent()).isEmpty();
        verifyNoInteractions(quotes);
    }

    @Test
    void storeManagerQueryIsCappedToOwnStore() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        Employee manager = employee(EmployeeRole.StoreManager, 21L);
        Store store = new Store();
        store.setStoreId(7L);
        manager.setStore(store);
        when(employees.findByEmpId(21L)).thenReturn(manager);
        when(quotes.findWorkbenchQuoteIds(
                eq(1L), eq(false), eq(7L), isNull(), isNull(), isNull(), eq(""), eq("%%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(
                new TenantContext(employees), quotes);

        assertThat(provider.call(manager).getContent()).isEmpty();
        verify(quotes).findWorkbenchQuoteIds(
                eq(1L), eq(false), eq(7L), isNull(), isNull(), isNull(), eq(""), eq("%%"), any(Pageable.class));
    }

    @Test
    void nonDesignerBusinessRoleIsRejectedBeforeRepositoryQuery() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        Employee finance = employee(EmployeeRole.Finance, 30L);
        when(employees.findByEmpId(30L)).thenReturn(finance);

        var provider = new com.th4.edge.decoboon.statistic.service.workbench.quote.page.support.Page(
                new TenantContext(employees), quotes);

        assertThatThrownBy(() -> provider.call(finance))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权执行");
        verifyNoInteractions(quotes);
    }

    private Employee employee(EmployeeRole role, Long empId) {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }

    private Quote quote(Long quoteId, Long customerId, Long propertyId, Employee designer) {
        Customer customer = new Customer();
        customer.setCustId(customerId);
        customer.setName("客户" + customerId);
        customer.setTelNo("13800000000");
        customer.setStatus(CustStatus.Following);
        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setCommunity("测试小区");
        property.setCustomer(customer);
        Quote quote = new Quote();
        quote.setQuoteId(quoteId);
        quote.setNo("Q" + quoteId);
        quote.setVersion(2);
        quote.setAmount(new BigDecimal("100000.00"));
        quote.setStatus(QuoteStatus.Draft);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(designer);
        return quote;
    }
}
