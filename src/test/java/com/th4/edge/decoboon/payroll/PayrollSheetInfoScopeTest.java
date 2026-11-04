package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollSheetScopeContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.service.sheets.info.support.Info;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollSheetInfoScopeTest {

    @Test
    void shouldReadSheetThroughDataScopeSpecification() {
        PayrollSheet sheet = sheet();
        PayrollSheetRepository repository = mock(PayrollSheetRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        PayrollSheetScopeContext scope = mock(PayrollSheetScopeContext.class);
        Employee manager = employee();
        when(access.requireManager(manager)).thenReturn(manager);
        when(access.visibleSheets(manager)).thenReturn(mock(Specification.class));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(sheet));
        when(storeFilter.resolve(manager, null)).thenReturn(null);
        when(scope.scope(100L, sheet, null)).thenReturn(sheet);

        Info provider = new Info(repository, access, storeFilter, scope);
        ReflectionTestUtils.setField(provider, "employee", manager);
        ReflectionTestUtils.setField(provider, "sheetId", 10L);

        assertThat(provider.call(new PayrollSheet())).isSameAs(sheet);
    }

    @Test
    void shouldHideSheetOutsideVisibleScope() {
        PayrollSheetRepository repository = mock(PayrollSheetRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        PayrollSheetScopeContext scope = mock(PayrollSheetScopeContext.class);
        Employee manager = employee();
        when(access.requireManager(manager)).thenReturn(manager);
        when(access.visibleSheets(manager)).thenReturn(mock(Specification.class));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        Info provider = new Info(repository, access, storeFilter, scope);
        ReflectionTestUtils.setField(provider, "employee", manager);
        ReflectionTestUtils.setField(provider, "sheetId", 10L);

        assertThatThrownBy(() -> provider.call(new PayrollSheet()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不存在或无权访问");
    }

    private PayrollSheet sheet() {
        Company company = new Company();
        company.setComId(100L);
        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(10L);
        sheet.setCompany(company);
        sheet.setPeriod("2026-09");
        return sheet;
    }

    private Employee employee() {
        Company company = new Company();
        company.setComId(100L);
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setRole(EmployeeRole.StoreManager);
        employee.setCompany(company);
        return employee;
    }
}
