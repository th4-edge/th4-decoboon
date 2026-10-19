package com.th4.edge.decoboon.catalog;

import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-075：报价项目库批量导入允许企业负责人与店长。 */
class CatalogImportContextTest {

    private EmployeeRepository employeeRepository;
    private CatalogImportContext context;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        context = new CatalogImportContext(mock(CatalogItemRepository.class), new TenantContext(employeeRepository));
    }

    @Test
    void shouldAllowBossAndStoreManagerButRejectMarketer() {
        Employee boss = employee(EmployeeRole.Boss);
        when(employeeRepository.findByEmpId(boss.getEmpId())).thenReturn(boss);
        assertThat(context.requireMaintainer(boss).getRole()).isEqualTo(EmployeeRole.Boss);

        Employee manager = employee(EmployeeRole.StoreManager);
        when(employeeRepository.findByEmpId(manager.getEmpId())).thenReturn(manager);
        assertThat(context.requireMaintainer(manager).getRole()).isEqualTo(EmployeeRole.StoreManager);

        Employee marketer = employee(EmployeeRole.Marketer);
        when(employeeRepository.findByEmpId(marketer.getEmpId())).thenReturn(marketer);
        assertThatThrownBy(() -> context.requireMaintainer(marketer))
                .hasMessageContaining("只有企业负责人或店长");
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setName(role.getName());
        employee.setRole(role);
        return employee;
    }
}
