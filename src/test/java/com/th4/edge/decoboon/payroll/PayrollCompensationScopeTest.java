package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollCompensationScopeTest {

    @Test
    void storeFinanceCanResolveOwnStoreEmployeeButNotOtherStore() {
        Fixture fixture = fixture(DataScopeType.Store, 10L, 100L);
        when(fixture.employees.findByEmpIdAndCompany_ComId(2L, 100L)).thenReturn(fixture.otherStore);
        when(fixture.employees.findByEmpIdAndCompany_ComId(3L, 100L)).thenReturn(fixture.ownStore);

        assertThatThrownBy(() -> fixture.access.resolveEmployee(fixture.finance, 2L))
                .hasMessageContaining("员工不存在或无权访问");
        assertThat(fixture.access.resolveEmployee(fixture.finance, 3L)).isSameAs(fixture.ownStore);
    }

    @Test
    void companyFinanceCanResolveAnyStoreEmployee() {
        Fixture fixture = fixture(DataScopeType.Company, null, 100L);
        when(fixture.employees.findByEmpIdAndCompany_ComId(2L, 100L)).thenReturn(fixture.otherStore);

        assertThat(fixture.access.resolveEmployee(fixture.finance, 2L)).isSameAs(fixture.otherStore);
    }

    @Test
    void employeePickerMustFilterStoreScopedFinanceToOwnStore() {
        Fixture fixture = fixture(DataScopeType.Store, 10L, 100L);
        CompensationSchemeRepository schemes = mock(CompensationSchemeRepository.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        when(storeFilter.resolve(fixture.finance, null)).thenReturn(10L);
        when(fixture.employees.findByCompany_ComIdAndStatusOrderByNameAsc(100L, EmployeeStatus.Active))
                .thenReturn(List.of(fixture.otherStore, fixture.ownStore));

        var page = new com.th4.edge.decoboon.payroll.service.employees.page.support.Page(
                fixture.employees, fixture.access);
        ReflectionTestUtils.setField(page, "employee", fixture.finance);
        ReflectionTestUtils.setField(page, "storeFilterContext", storeFilter);

        Page<Employee> result = page.call(new Employee());

        assertThat(result.getContent()).extracting(Employee::getEmpId).containsExactly(3L);
    }

    private Fixture fixture(DataScopeType type, Long scopeStoreId, Long comId) {
        Company company = new Company();
        company.setComId(comId);
        Store otherStore = store(20L);
        Store ownStore = store(scopeStoreId == null ? 20L : scopeStoreId);
        Employee finance = employee(1L, company, ownStore);
        Employee targetOther = employee(2L, company, otherStore);
        Employee targetOwn = employee(3L, company, ownStore);

        TenantContext tenant = mock(TenantContext.class);
        DataScopeContext scope = mock(DataScopeContext.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        PayrollAccessContext access = new PayrollAccessContext(tenant, scope, employees, projects, mock(com.th4.edge.decoboon.payroll.repository.PayrollItemRepository.class));
        when(tenant.requireRole(eq(finance), eq(EmployeeRole.Boss), eq(EmployeeRole.Finance))).thenReturn(finance);
        when(tenant.requireRole(eq(finance), eq(EmployeeRole.Boss), eq(EmployeeRole.Finance),
                eq(EmployeeRole.StoreManager))).thenReturn(finance);
        when(scope.resolve(finance)).thenReturn(new DataScope(type, comId, scopeStoreId, null, 1L));
        return new Fixture(access, employees, finance, targetOther, targetOwn);
    }

    private Employee employee(Long empId, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(EmployeeRole.Finance);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        employee.setStore(store);
        return employee;
    }

    private Store store(Long storeId) {
        Store store = new Store();
        store.setStoreId(storeId);
        return store;
    }

    private record Fixture(PayrollAccessContext access, EmployeeRepository employees,
                           Employee finance, Employee otherStore, Employee ownStore) {
    }
}
