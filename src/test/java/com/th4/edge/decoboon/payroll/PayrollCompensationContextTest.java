package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollCompensationAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollCompensationContextTest {

    @Test
    void shouldBatchMarkLockedEmployeesWithoutPerRowQueries() {
        CompensationSchemeRepository schemes = mock(CompensationSchemeRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollItemRepository payrollItems = mock(PayrollItemRepository.class);
        CommissionItemRepository commissions = mock(CommissionItemRepository.class);
        PayrollCompensationContext context = new PayrollCompensationContext(
                schemes, attendances, payrollItems, commissions);

        CompensationScheme first = scheme(10L, 1L);
        CompensationScheme second = scheme(20L, 2L);
        when(attendances.findEmployeeIdsWithFacts(100L, Set.of(1L, 2L))).thenReturn(Set.of(1L));
        when(payrollItems.findEmployeeIdsWithFacts(100L, Set.of(1L, 2L))).thenReturn(Set.of());
        when(commissions.findEmployeeIdsWithFacts(100L, Set.of(1L, 2L))).thenReturn(Set.of(2L));

        context.enrichLockState(List.of(first, second), 100L);

        assertThat(first.getLocked()).isTrue();
        assertThat(first.getLockedReason()).contains("后续方案");
        assertThat(second.getLocked()).isTrue();
    }

    @Test
    void shouldValidateSalaryScheduleAndDateRange() {
        PayrollCompensationContext context = new PayrollCompensationContext(
                mock(CompensationSchemeRepository.class),
                mock(AttendanceSummaryRepository.class),
                mock(PayrollItemRepository.class),
                mock(CommissionItemRepository.class));

        assertThatThrownBy(() -> context.validBaseSalary(BigDecimal.ZERO))
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> context.validScheduledDays(32))
                .hasMessageContaining("1 至 31");
        assertThatThrownBy(() -> context.validPeriod(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 9)))
                .hasMessageContaining("不能早于");
        assertThat(context.validBaseSalary(new BigDecimal("3000"))).isEqualByComparingTo("3000.00");
        assertThat(context.validScheduledDays(26)).isEqualTo(26);
    }

    @Test
    void auditSnapshotMustContainSalaryFacts() {
        CompensationScheme scheme = scheme(10L, 1L);
        scheme.setBaseSalary(new BigDecimal("3000.00"));
        scheme.setScheduledDays(26);
        scheme.setEffectiveFrom(LocalDate.of(2026, 10, 1));
        scheme.setEffectiveTo(LocalDate.of(2026, 12, 31));

        String snapshot = new PayrollCompensationAuditContext().snapshot(scheme);

        assertThat(snapshot).contains("baseSalary=3000.00", "scheduledDays=26",
                "effectiveFrom=2026-10-01", "effectiveTo=2026-12-31");
    }

    private CompensationScheme scheme(Long schemeId, Long employeeId) {
        Employee employee = new Employee();
        employee.setEmpId(employeeId);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setSchemeId(schemeId);
        scheme.setEmployee(employee);
        return scheme;
    }
}
