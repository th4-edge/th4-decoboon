package com.th4.edge.tenant.user;

import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-032 / BR-028：BOSS 是内部岗位能力超集，但不是治理和外部能力集合。 */
class TenantInternalRoleTest {

    private EmployeeRepository employeeRepository;
    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        tenantContext = new TenantContext(employeeRepository);
    }

    @Test
    void shouldAllowBossToUseInternalRoleCapability() {
        Employee boss = employee(EmployeeRole.Boss);
        when(employeeRepository.findByEmpId(boss.getEmpId())).thenReturn(boss);

        Employee current = tenantContext.requireInternalRole(boss, EmployeeRole.Pm);

        assertThat(current).isSameAs(boss);
    }

    @Test
    void shouldNotExpandOrdinaryRoleCapability() {
        Employee designer = employee(EmployeeRole.Designer);
        when(employeeRepository.findByEmpId(designer.getEmpId())).thenReturn(designer);

        assertThatThrownBy(() -> tenantContext.requireInternalRole(designer, EmployeeRole.Pm))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权执行");
    }

    @Test
    void shouldKeepExactRoleCheckForGovernanceActions() {
        Employee boss = employee(EmployeeRole.Boss);
        when(employeeRepository.findByEmpId(boss.getEmpId())).thenReturn(boss);

        assertThatThrownBy(() -> tenantContext.requireRole(boss, EmployeeRole.StoreManager))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权执行");
    }

    @Test
    void frozenTeamLeaderMustNotPassTenantBoundaryEvenWithAnOldToken() {
        Employee leader = employee(EmployeeRole.TeamLeader);
        when(employeeRepository.findByEmpId(leader.getEmpId())).thenReturn(leader);

        assertThatThrownBy(() -> tenantContext.requireEmployee(leader))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 100);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        return employee;
    }
}
