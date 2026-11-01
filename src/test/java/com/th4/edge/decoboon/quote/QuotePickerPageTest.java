package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.service.quotes.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 客户确认报价选择器必须使用数据库分页，且选择器硬上限为 50。 */
class QuotePickerPageTest {

    @Test
    void acceptedQuotePickerUsesDatabasePaginationAndCapsPageSize() {
        QuoteRepository repository = mock(QuoteRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        Employee boss = employee();
        when(tenant.requireEmployee(boss)).thenReturn(boss);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Quote>(List.of(), invocation.getArgument(1), 0));

        Page provider = new Page(repository, tenant);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "current", 2);
        ReflectionTestUtils.setField(provider, "size", 200);
        ReflectionTestUtils.setField(provider, "status", QuoteStatus.Accepted);
        ReflectionTestUtils.setField(provider, "customerId", 10L);
        ReflectionTestUtils.setField(provider, "propertyId", 20L);
        ReflectionTestUtils.setField(provider, "keyword", "阳光");

        assertThat(provider.call(new Quote()).getTotalElements()).isZero();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().getOrderFor("quoteId")).isNotNull();
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setRole(EmployeeRole.Boss);
        employee.setCompany(company);
        return employee;
    }
}
