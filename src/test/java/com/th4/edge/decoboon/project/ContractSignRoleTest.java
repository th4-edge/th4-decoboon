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

/** 合同发起、签约确认和生效立项角色矩阵回归。 */
class ContractSignRoleTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ProjectAccessContext context = new ProjectAccessContext(tenantContext, null, null, null);

    @Test
    void shouldAllowDesignerBossAndStoreManagerToPrepareContract() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Designer}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThat(context.requireContractPreparer(employee)).isSameAs(employee);
        }
    }

    @Test
    void shouldAllowBossAndStoreManagerToConfirmAndCreateProject() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Boss, EmployeeRole.StoreManager}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThat(context.requireContractConfirmer(employee)).isSameAs(employee);
            assertThat(context.requireProjectCreator(employee)).isSameAs(employee);
        }
    }

    @Test
    void shouldRejectOtherRolesFromPreparingContract() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Pm, EmployeeRole.Finance,
                EmployeeRole.Marketer, EmployeeRole.TeamLeader}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThatThrownBy(() -> context.requireContractPreparer(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("只有设计师、店长或企业负责人");
        }
    }

    @Test
    void designerMustNotConfirmContractOrCreateProject() {
        Employee designer = employee(EmployeeRole.Designer);
        when(tenantContext.requireEmployee(designer)).thenReturn(designer);
        assertThatThrownBy(() -> context.requireContractConfirmer(designer))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有企业负责人或店长");
        assertThatThrownBy(() -> context.requireProjectCreator(designer))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有企业负责人或店长");
    }

    @Test
    void otherRolesMustNotConfirmContractOrCreateProject() {
        for (EmployeeRole role : new EmployeeRole[]{
                EmployeeRole.Pm, EmployeeRole.Finance, EmployeeRole.Marketer, EmployeeRole.TeamLeader}) {
            Employee employee = employee(role);
            when(tenantContext.requireEmployee(employee)).thenReturn(employee);
            assertThatThrownBy(() -> context.requireContractConfirmer(employee))
                    .isInstanceOf(DefinedException.class)
                    .hasMessageContaining("只有企业负责人或店长");
            assertThatThrownBy(() -> context.requireProjectCreator(employee))
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
