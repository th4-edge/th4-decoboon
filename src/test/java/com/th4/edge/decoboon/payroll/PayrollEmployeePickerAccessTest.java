package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 费用代填员工选择器必须允许店长加载本店员工，并由数据库完成分页。 */
class PayrollEmployeePickerAccessTest {

    @Test
    void storeManagerCanLoadSameStoreEmployeeCandidates() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        Company company = company(1L);
        Store first = store(10L);
        Employee manager = employee(3L, EmployeeRole.StoreManager, company, first);
        Employee sameStore = employee(4L, EmployeeRole.Marketer, company, first);
        when(access.requireManager(manager)).thenReturn(manager);
        when(storeFilter.resolve(manager, null)).thenReturn(10L);
        when(repository.findPickerPage(eq(1L), eq(10L), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sameStore)));
        var page = new com.th4.edge.decoboon.payroll.service.employees.page.support.Page(repository, access);
        ReflectionTestUtils.setField(page, "employee", manager);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);
        ReflectionTestUtils.setField(page, "storeFilterContext", storeFilter);

        var result = page.call(manager);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(sameStore);
        verify(access).requireManager(manager);
        verify(storeFilter).resolve(manager, null);
        verify(repository).findPickerPage(eq(1L), eq(10L), any(), any(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private static Store store(Long id) {
        Store store = new Store();
        store.setStoreId(id);
        return store;
    }

    private static Employee employee(Long id, EmployeeRole role, Company company, Store store) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setStatus(EmployeeStatus.Active);
        return employee;
    }
}
