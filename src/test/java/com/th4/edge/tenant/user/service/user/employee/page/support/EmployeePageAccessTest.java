package com.th4.edge.tenant.user.service.user.employee.page.support;

import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 财务客户负责人候选的员工目录权限和数据范围回归。 */
class EmployeePageAccessTest {

    @Test
    void headquartersFinanceCanLoadCompanyEmployeeCandidates() {
        TenantContext tenant = mock(TenantContext.class);
        EmployeeRepository repository = mock(EmployeeRepository.class);
        DataScopeContext scopes = mock(DataScopeContext.class);
        Employee finance = employee(7L, EmployeeRole.Finance, 1L, 10L);
        org.springframework.data.domain.Page<Employee> expected = new PageImpl<>(List.of(finance));
        when(tenant.requireRole(eq(finance), any(EmployeeRole[].class))).thenReturn(finance);
        when(scopes.resolve(finance)).thenReturn(new DataScope(DataScopeType.Company, 1L, 10L, null, 7L));
        when(repository.findPickerPage(eq(1L), isNull(), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(expected);
        Page provider = provider(tenant, repository, scopes, finance);

        assertThat(provider.call(finance)).isSameAs(expected);
        verify(repository).findPickerPage(eq(1L), isNull(), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void storeFinanceCanOnlyLoadSameStoreEmployeeCandidates() {
        TenantContext tenant = mock(TenantContext.class);
        EmployeeRepository repository = mock(EmployeeRepository.class);
        DataScopeContext scopes = mock(DataScopeContext.class);
        Employee finance = employee(7L, EmployeeRole.Finance, 1L, 20L);
        org.springframework.data.domain.Page<Employee> expected = new PageImpl<>(List.of(finance));
        when(tenant.requireRole(eq(finance), any(EmployeeRole[].class))).thenReturn(finance);
        when(scopes.resolve(finance)).thenReturn(new DataScope(DataScopeType.Store, 1L, 20L, null, 7L));
        when(repository.findPickerPage(eq(1L), eq(20L), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(expected);
        Page provider = provider(tenant, repository, scopes, finance);

        assertThat(provider.call(finance)).isSameAs(expected);
        verify(repository).findPickerPage(eq(1L), eq(20L), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void storeFinanceCannotRequestAnotherStore() {
        TenantContext tenant = mock(TenantContext.class);
        EmployeeRepository repository = mock(EmployeeRepository.class);
        DataScopeContext scopes = mock(DataScopeContext.class);
        Employee finance = employee(7L, EmployeeRole.Finance, 1L, 20L);
        when(tenant.requireRole(eq(finance), any(EmployeeRole[].class))).thenReturn(finance);
        when(scopes.resolve(finance)).thenReturn(new DataScope(DataScopeType.Store, 1L, 20L, null, 7L));
        Page provider = provider(tenant, repository, scopes, finance);
        ReflectionTestUtils.setField(provider, "storeId", 30L);

        assertThatThrownBy(() -> provider.call(finance))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("其他门店");
    }

    private static Page provider(TenantContext tenant, EmployeeRepository repository,
                                 DataScopeContext scopes, Employee principal) {
        Page provider = new Page(tenant, repository, scopes);
        ReflectionTestUtils.setField(provider, "employee", principal);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        return provider;
    }

    private static Employee employee(Long empId, EmployeeRole role, Long companyId, Long storeId) {
        Company company = new Company();
        company.setComId(companyId);
        Store store = new Store();
        store.setStoreId(storeId);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store);
        return employee;
    }
}
