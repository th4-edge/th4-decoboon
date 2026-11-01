package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollWorkbenchAggregateContext;
import com.th4.edge.decoboon.payroll.context.PayrollWorkbenchContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 薪酬工作台只负责范围解析、聚合结果组装和响应投影。 */
class PayrollWorkbenchContextTest {

    @Test
    void shouldProjectLatestSheetWhenSelectedPeriodHasNoSheet() {
        Employee boss = boss();
        PayrollAccessContext access = access(boss);
        PayrollWorkbenchAggregateContext aggregate = mock(PayrollWorkbenchAggregateContext.class);
        StoreFilterContext scope = mock(StoreFilterContext.class);
        when(scope.resolve(boss, null)).thenReturn(0L);
        when(aggregate.attendance(boss, "2026-10", 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.AttendanceAggregate(0));
        when(aggregate.sheets(boss, "2026-10", 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.SheetAggregate(0, 0, 0, 0, 0, false));
        when(aggregate.payables(boss, "2026-10", 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.PayableAggregate(BigDecimal.ZERO));
        when(aggregate.commissions(boss, 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.CommissionAggregate(BigDecimal.ZERO));
        when(aggregate.payslips(boss, "2026-10", 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.PayslipAggregate(0, 0));
        when(aggregate.latestSheet(boss, 0L))
                .thenReturn(new PayrollWorkbenchAggregateContext.LatestSheet(
                        9001L, "2027-01", PayrollSheetStatus.Paid, new BigDecimal("5727.27")));

        var context = new PayrollWorkbenchContext(access, aggregate, scope);
        var report = context.report(boss, "2026-10");

        assertThat(report.getCurrentPeriodHasSheet()).isFalse();
        assertThat(report.getLatestSheetId()).isEqualTo(9001L);
        assertThat(report.getLatestSheetPeriod()).isEqualTo("2027-01");
        assertThat(report.getLatestSheetStatus()).isEqualTo(PayrollSheetStatus.Paid);
        assertThat(report.getLatestSheetTotalNetAmount()).isEqualByComparingTo("5727.27");
    }

    @Test
    void shouldMapAggregateResultsWithoutLoadingEntities() {
        Employee boss = boss();
        PayrollAccessContext access = access(boss);
        PayrollWorkbenchAggregateContext aggregate = mock(PayrollWorkbenchAggregateContext.class);
        StoreFilterContext scope = mock(StoreFilterContext.class);
        when(scope.resolve(boss, 10L)).thenReturn(10L);
        when(aggregate.attendance(boss, "2026-10", 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.AttendanceAggregate(2));
        when(aggregate.sheets(boss, "2026-10", 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.SheetAggregate(1, 2, 3, 4, 5, true));
        when(aggregate.payables(boss, "2026-10", 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.PayableAggregate(new BigDecimal("130.00")));
        when(aggregate.commissions(boss, 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.CommissionAggregate(new BigDecimal("80.00")));
        when(aggregate.payslips(boss, "2026-10", 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.PayslipAggregate(6, 7));
        when(aggregate.latestSheet(boss, 10L))
                .thenReturn(new PayrollWorkbenchAggregateContext.LatestSheet(
                        11L, "2027-01", PayrollSheetStatus.Pending, new BigDecimal("130.00")));

        var context = new PayrollWorkbenchContext(access, aggregate, scope);
        var report = context.report(boss, "2026-10", 10L);

        assertThat(report.getPendingAttendanceCount()).isEqualTo(2);
        assertThat(report.getCurrentPeriodHasSheet()).isTrue();
        assertThat(report.getDraftSheetCount()).isEqualTo(1);
        assertThat(report.getPendingSheetCount()).isEqualTo(2);
        assertThat(report.getConfirmedSheetCount()).isEqualTo(3);
        assertThat(report.getPartialPaidSheetCount()).isEqualTo(4);
        assertThat(report.getPaidSheetCount()).isEqualTo(5);
        assertThat(report.getPendingEmployeePayableAmount()).isEqualByComparingTo("130.00");
        assertThat(report.getReleasedUnpaidCommissionAmount()).isEqualByComparingTo("80.00");
        assertThat(report.getPendingPayslipCount()).isEqualTo(6);
        assertThat(report.getPublishedPayslipCount()).isEqualTo(7);
        assertThat(report.getLatestSheetId()).isEqualTo(11L);
        verify(scope).resolve(boss, 10L);
    }

    private PayrollAccessContext access(Employee boss) {
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireManager(boss)).thenReturn(boss);
        return access;
    }

    private Employee boss() {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setRole(EmployeeRole.Boss);
        return employee;
    }
}
