package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefinedException;
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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-097 / DEC-098：店长与财务的薪酬高危动作按门店/企业数据范围收敛。 */
class PayrollManagerActionAccessTest {

    @Test
    void storeManagerCanUseOwnStorePayrollActions() {
        Fixture fixture = fixture(EmployeeRole.StoreManager, 10L, DataScopeType.Store);
        when(fixture.employees.findByEmpId(1L)).thenReturn(fixture.current);

        assertThat(fixture.access.requireConfirmActor(fixture.principal)).isSameAs(fixture.current);
        assertThat(fixture.access.requireReturnActor(fixture.principal)).isSameAs(fixture.current);
        assertThat(fixture.access.requireDeleteActor(fixture.principal)).isSameAs(fixture.current);
        assertThat(fixture.access.requirePaymentActor(fixture.principal)).isSameAs(fixture.current);
        fixture.access.requireStoreItemsInStore(fixture.current, List.of(item(10L)));
    }

    @Test
    void storeManagerCannotTouchMixedStoreSheet() {
        Fixture fixture = fixture(EmployeeRole.StoreManager, 10L, DataScopeType.Store);

        assertThatThrownBy(() -> fixture.access.requireStoreItemsInStore(
                fixture.current, List.of(item(10L), item(20L))))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("其他门店");
    }

    @Test
    void storeFinanceCanDeleteAndPayButCannotConfirmOrReturn() {
        Fixture fixture = fixture(EmployeeRole.Finance, 10L, DataScopeType.Store);
        when(fixture.employees.findByEmpId(1L)).thenReturn(fixture.current);

        assertThat(fixture.access.requirePaymentActor(fixture.principal)).isSameAs(fixture.current);
        assertThat(fixture.access.requireDeleteActor(fixture.principal)).isSameAs(fixture.current);
        assertThatThrownBy(() -> fixture.access.requireConfirmActor(fixture.principal))
                .isInstanceOf(DefinedException.class);
        assertThatThrownBy(() -> fixture.access.requireReturnActor(fixture.principal))
                .isInstanceOf(DefinedException.class);
    }

    @Test
    void storeFinanceCanDeleteOnlyOwnStorePayrollItems() {
        Fixture fixture = fixture(EmployeeRole.Finance, 10L, DataScopeType.Store);

        fixture.access.requireStoreItemsInStore(fixture.current, List.of(item(10L)));
        assertThatThrownBy(() -> fixture.access.requireStoreItemsInStore(
                fixture.current, List.of(item(10L), item(20L))))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("其他门店");
    }

    @Test
    void headquartersFinanceCanDeleteMixedStoreDraftWithinCompany() {
        Fixture fixture = fixture(EmployeeRole.Finance, 10L, DataScopeType.Company);
        when(fixture.employees.findByEmpId(1L)).thenReturn(fixture.current);

        assertThat(fixture.access.requireDeleteActor(fixture.principal)).isSameAs(fixture.current);
        fixture.access.requireStoreItemsInStore(fixture.current, List.of(item(10L), item(20L)));
    }

    private Fixture fixture(EmployeeRole role, Long storeId, DataScopeType scopeType) {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        Employee current = new Employee();
        current.setEmpId(1L);
        current.setRole(role);
        current.setStatus(EmployeeStatus.Active);
        Company company = new Company();
        company.setComId(100L);
        current.setCompany(company);
        Store store = new Store();
        store.setStoreId(storeId);
        current.setStore(store);
        DataScopeContext dataScope = mock(DataScopeContext.class);
        when(dataScope.resolve(current)).thenReturn(new DataScope(scopeType, 100L, storeId, null, 1L));
        PayrollAccessContext access = new PayrollAccessContext(
                new TenantContext(employees),
                dataScope,
                employees,
                mock(ProjectRepository.class),
                mock(PayrollItemRepository.class));
        Employee principal = new Employee();
        principal.setEmpId(1L);
        return new Fixture(access, employees, principal, current);
    }

    private PayrollItem item(Long storeId) {
        Store store = new Store();
        store.setStoreId(storeId);
        PayrollItem item = new PayrollItem();
        item.setStore(store);
        return item;
    }

    private record Fixture(PayrollAccessContext access,
                           EmployeeRepository employees,
                           Employee principal,
                           Employee current) {
    }
}
