package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignWritePermissionTest {

    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);
    private final DesignContext context = new DesignContext(
            dataScopeContext, null, null, null, null, null, null);

    @Test
    void shouldRejectRolesWithoutDesignWriteCapability() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Marketer, EmployeeRole.Pm, EmployeeRole.Finance}) {
            Employee employee = employee(role);
            when(dataScopeContext.requireEmployee(employee)).thenReturn(employee);

            assertThatThrownBy(() -> context.requireDesignWriteAccess(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("无权新增或复制设计方案");
        }
    }

    @Test
    void shouldAllowDesignWriteRoles() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.TeamLeader, EmployeeRole.Designer}) {
            Employee employee = employee(role);
            when(dataScopeContext.requireEmployee(employee)).thenReturn(employee);

            assertThat(context.requireDesignWriteAccess(employee)).isSameAs(employee);
        }
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setName(role.name());
        employee.setRole(role);
        return employee;
    }
}
