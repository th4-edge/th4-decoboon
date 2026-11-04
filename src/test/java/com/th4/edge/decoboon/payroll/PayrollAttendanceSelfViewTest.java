package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.service.attendances.summary.page.support.Page;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BR-038：员工本人只能读取本人考勤，管理角色可显式进入本人视角。 */
class PayrollAttendanceSelfViewTest {

    @Test
    void ordinaryEmployeeIsAlwaysRestrictedToSelfEvenWithoutSelfOnly() {
        Fixture fixture = fixture(false, null);
        AttendanceSummary own = attendance(101L, fixture.current(), 9L, "2026-10");
        when(fixture.repository().findAll(eq(fixture.visible()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(own)));

        var result = fixture.provider().call(new AttendanceSummary());

        assertThat(result.getContent()).containsExactly(own);
        verify(fixture.storeFilter(), never()).resolve(any(), any());
        verify(fixture.repository()).findAll(eq(fixture.visible()), any(Pageable.class));
        verify(fixture.repository(), never()).findAll(any(Specification.class));
    }

    @Test
    void managerSelfOnlyIgnoresRequestedStoreAndReturnsOnlySelf() {
        Fixture fixture = fixture(true, 88L);
        ReflectionTestUtils.setField(fixture.provider(), "selfOnly", true);
        AttendanceSummary own = attendance(101L, fixture.current(), 19L, "2026-10");
        when(fixture.repository().findAll(eq(fixture.visible()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(own)));

        var result = fixture.provider().call(new AttendanceSummary());

        assertThat(result.getContent()).containsExactly(own);
        verify(fixture.storeFilter(), never()).resolve(any(), any());
    }

    @Test
    void managerManagementViewKeepsExistingStoreScope() {
        Fixture fixture = fixture(true, 19L);
        AttendanceSummary sameStore = attendance(101L, employee(2L, EmployeeRole.Designer, 19L), 19L, "2026-10");
        when(fixture.repository().findAll(eq(fixture.visible()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sameStore)));
        when(fixture.storeFilter().resolve(fixture.current(), 19L)).thenReturn(19L);

        var result = fixture.provider().call(new AttendanceSummary());

        assertThat(result.getContent()).containsExactly(sameStore);
        verify(fixture.storeFilter()).resolve(fixture.current(), 19L);
    }

    @Test
    void pageSizeIsCappedAtFifty() {
        Fixture fixture = fixture(true, null);
        ReflectionTestUtils.setField(fixture.provider(), "size", 200);
        when(fixture.storeFilter().resolve(fixture.current(), null)).thenReturn(0L);
        when(fixture.repository().findAll(eq(fixture.visible()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        fixture.provider().call(new AttendanceSummary());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fixture.repository()).findAll(eq(fixture.visible()), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(boolean manager, Long requestedStoreId) {
        AttendanceSummaryRepository repository = mock(AttendanceSummaryRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        StoreFilterContext storeFilter = mock(StoreFilterContext.class);
        Employee current = employee(1L, manager ? EmployeeRole.StoreManager : EmployeeRole.Designer, 19L);
        Specification<AttendanceSummary> visible = mock(Specification.class);
        when(visible.and(any(Specification.class))).thenReturn(visible);
        when(access.requireAttendanceViewer(current)).thenReturn(current);
        when(access.isPayrollManager(current)).thenReturn(manager);
        when(access.visible(current, AttendanceSummary.class)).thenReturn(visible);

        Page provider = new Page(repository, access, storeFilter);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "month", "2026-10");
        ReflectionTestUtils.setField(provider, "current", 1);
        ReflectionTestUtils.setField(provider, "size", 20);
        ReflectionTestUtils.setField(provider, "storeId", requestedStoreId);
        return new Fixture(repository, storeFilter, current, visible, provider);
    }

    private Employee employee(Long empId, EmployeeRole role, Long storeId) {
        Store store = new Store();
        store.setStoreId(storeId);
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setStore(store);
        return employee;
    }

    private AttendanceSummary attendance(Long summaryId, Employee employee, Long storeId, String month) {
        Store store = new Store();
        store.setStoreId(storeId);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setSummaryId(summaryId);
        summary.setEmployee(employee);
        summary.setStore(store);
        summary.setPayrollMonth(month);
        return summary;
    }

    private record Fixture(AttendanceSummaryRepository repository,
                           StoreFilterContext storeFilter,
                           Employee current,
                           Specification<AttendanceSummary> visible,
                           Page provider) {
    }
}
