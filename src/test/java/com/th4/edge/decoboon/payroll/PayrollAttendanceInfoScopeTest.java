package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.service.attendances.summary.info.support.Info;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollAttendanceInfoScopeTest {

    @Test
    void shouldResolveAttendanceThroughDataScopeSpecification() {
        AttendanceSummaryRepository repository = mock(AttendanceSummaryRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Employee current = employee(1L);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setSummaryId(9L);
        @SuppressWarnings("unchecked")
        Specification<AttendanceSummary> visible = mock(Specification.class);
        when(access.requireManager(current)).thenReturn(current);
        when(access.visible(current, AttendanceSummary.class)).thenReturn(visible);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(summary));

        Info provider = new Info(repository, access);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "summaryId", 9L);

        assertThat(provider.call(new AttendanceSummary())).isSameAs(summary);
        verify(access).visible(current, AttendanceSummary.class);
        verify(repository).findOne(any(Specification.class));
        verify(repository, never()).findById(any());
    }

    @Test
    void shouldHideAttendanceWhenScopedLookupDoesNotMatch() {
        AttendanceSummaryRepository repository = mock(AttendanceSummaryRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Employee current = employee(1L);
        @SuppressWarnings("unchecked")
        Specification<AttendanceSummary> visible = mock(Specification.class);
        when(access.requireManager(current)).thenReturn(current);
        when(access.visible(current, AttendanceSummary.class)).thenReturn(visible);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        Info provider = new Info(repository, access);
        ReflectionTestUtils.setField(provider, "employee", current);
        ReflectionTestUtils.setField(provider, "summaryId", 9L);

        assertThatThrownBy(() -> provider.call(new AttendanceSummary()))
                .hasMessageContaining("考勤不存在或无权访问");
    }

    private Employee employee(Long empId) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        return employee;
    }
}
