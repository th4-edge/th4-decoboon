package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollAttendanceAccessContextTest {

    @Test
    void ordinaryEmployeeMustResolveToSelfScope() {
        Fixture fixture = fixture(EmployeeRole.Designer);
        when(fixture.dataScope.resolve(fixture.current))
                .thenReturn(new DataScope(DataScopeType.Self, 100L, 10L, null, 1L));

        assertThat(fixture.access.requireAttendanceViewer(fixture.principal)).isSameAs(fixture.current);
        verify(fixture.dataScope).resolve(fixture.current);
    }

    @Test
    void ordinaryEmployeeWithUnexpectedNonSelfScopeIsRejected() {
        Fixture fixture = fixture(EmployeeRole.Designer);
        when(fixture.dataScope.resolve(fixture.current))
                .thenReturn(new DataScope(DataScopeType.Team, 100L, 10L, 20L, 1L));

        assertThatThrownBy(() -> fixture.access.requireAttendanceViewer(fixture.principal))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("考勤数据范围");
    }

    @Test
    void payrollManagerKeepsManagementViewerRole() {
        Fixture fixture = fixture(EmployeeRole.StoreManager);

        assertThat(fixture.access.requireAttendanceViewer(fixture.principal)).isSameAs(fixture.current);
        verify(fixture.dataScope, never()).resolve(fixture.current);
    }

    private Fixture fixture(EmployeeRole role) {
        TenantContext tenant = mock(TenantContext.class);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        Employee principal = new Employee();
        principal.setEmpId(1L);
        Employee current = new Employee();
        current.setEmpId(1L);
        current.setRole(role);
        when(tenant.requireEmployee(principal)).thenReturn(current);
        PayrollAccessContext access = new PayrollAccessContext(
                tenant,
                dataScope,
                mock(EmployeeRepository.class),
                mock(ProjectRepository.class),
                mock(PayrollItemRepository.class));
        return new Fixture(access, dataScope, principal, current);
    }

    private record Fixture(PayrollAccessContext access,
                           DataScopeContext dataScope,
                           Employee principal,
                           Employee current) {
    }
}
