package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.service.commissions.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BR-038：员工本人只能读取本人提成，管理角色可显式进入本人视角。 */
class PayrollCommissionSelfViewTest {

    @Test
    void ordinaryEmployeePageDoesNotUseManagementStoreScope() {
        Fixture fixture = fixture(false, null);
        CommissionItem own = commission(101L, fixture.current);
        stubPage(fixture, own);

        var result = fixture.provider.call(new CommissionItem());

        assertThat(result.getContent()).containsExactly(own);
        verify(fixture.storeFilter, never()).resolve(any(), any());
    }

    @Test
    void managerSelfOnlyDoesNotUseStoreScope() {
        Fixture fixture = fixture(true, 88L);
        ReflectionTestUtils.setField(fixture.provider, "selfOnly", true);
        CommissionItem own = commission(101L, fixture.current);
        stubPage(fixture, own);

        var result = fixture.provider.call(new CommissionItem());

        assertThat(result.getContent()).containsExactly(own);
        verify(fixture.storeFilter, never()).resolve(any(), any());
    }

    @Test
    void managerManagementViewKeepsStoreScope() {
        Fixture fixture = fixture(true, 19L);
        CommissionItem own = commission(101L, fixture.current);
        stubPage(fixture, own);
        when(fixture.storeFilter.resolve(fixture.current, 19L)).thenReturn(19L);

        var result = fixture.provider.call(new CommissionItem());

        assertThat(result.getContent()).containsExactly(own);
        verify(fixture.storeFilter).resolve(fixture.current, 19L);
    }

    @SuppressWarnings("unchecked")
    private void stubPage(Fixture fixture, CommissionItem item) {
        when(fixture.repository.findAll(org.mockito.ArgumentMatchers.<Specification<CommissionItem>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(fixture.repository.findByCommissionIdIn(List.of(item.getCommissionId())))
                .thenReturn(List.of(item));
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(boolean manager, Long storeId) {
        CommissionItemRepository repository = mock(CommissionItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        Employee current = employee(1L, manager ? EmployeeRole.Finance : EmployeeRole.Designer);
        Specification<CommissionItem> visible = mock(Specification.class);
        when(access.requireCommissionViewer(current)).thenReturn(current);
        when(access.isPayrollManager(current)).thenReturn(manager);
        when(access.visible(current, CommissionItem.class)).thenReturn(visible);

        Page provider = new Page(repository, access);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "storeFilterContext", storeFilter);
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        ReflectionTestUtils.setField(provider, "storeId", storeId);
        return new Fixture(repository, storeFilter, current, provider);
    }

    private Employee employee(Long empId, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        return employee;
    }

    private CommissionItem commission(Long commissionId, Employee employee) {
        CommissionItem item = new CommissionItem();
        item.setCommissionId(commissionId);
        item.setEmployee(employee);
        return item;
    }

    private record Fixture(CommissionItemRepository repository,
                           StoreFilterContext storeFilter,
                           Employee current,
                           Page provider) {
    }
}
