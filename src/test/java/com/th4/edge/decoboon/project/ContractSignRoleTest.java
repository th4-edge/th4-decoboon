package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 合同签约/生效角色矩阵回归（2026-10-05 用户确认：企业负责人 + 店长）。
 */
class ContractSignRoleTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ProjectAccessContext context = new ProjectAccessContext(tenantContext, null, null, null);

    @Test
    void shouldAllowBossAndStoreManagerToSignContract() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Boss, EmployeeRole.StoreManager}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThat(context.requireContractSigner(employee)).isSameAs(employee);
        }
    }

    @Test
    void shouldRejectOtherRolesFromSigningContract() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Designer, EmployeeRole.Pm, EmployeeRole.Finance,
                EmployeeRole.Marketer, EmployeeRole.TeamLeader}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThatThrownBy(() -> context.requireContractSigner(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("只有企业负责人或店长");
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
