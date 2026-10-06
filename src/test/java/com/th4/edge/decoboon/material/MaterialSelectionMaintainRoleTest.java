package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DEC-035：Designer/Boss/StoreManager/Finance 可维护主材选材；Pm/TeamLeader 只读。
 */
class MaterialSelectionMaintainRoleTest {

    private PropertyContext propertyContext;
    private MaterialSelectionContext context;
    private Property property;

    @BeforeEach
    void setUp() {
        propertyContext = mock(PropertyContext.class);
        context = new MaterialSelectionContext(null, null, propertyContext, null, null, null, null);
        property = new Property();
        property.setPropertyId(1L);
    }

    @Test
    void designerBossStoreManagerAndFinanceCanMaintain() {
        Employee designer = employee(EmployeeRole.Designer, 10L);
        property.setDesigner(designer);
        when(propertyContext.canView(property, designer)).thenReturn(true);
        assertThat(context.canMaintain(property, designer)).isTrue();

        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance}) {
            Employee employee = employee(role, 100L + role.ordinal());
            when(propertyContext.canView(property, employee)).thenReturn(true);
            assertThat(context.canMaintain(property, employee)).as("角色 %s 应可维护", role).isTrue();
        }
    }

    @Test
    void pmTeamLeaderAndMarketerCannotMaintain() {
        for (EmployeeRole role : new EmployeeRole[]{EmployeeRole.Pm, EmployeeRole.TeamLeader, EmployeeRole.Marketer}) {
            Employee employee = employee(role, 200L + role.ordinal());
            when(propertyContext.canView(property, employee)).thenReturn(true);
            assertThat(context.canMaintain(property, employee)).as("角色 %s 应只读", role).isFalse();
        }
    }

    private Employee employee(EmployeeRole role, Long empId) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        return employee;
    }
}
