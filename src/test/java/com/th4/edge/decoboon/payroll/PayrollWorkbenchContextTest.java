package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollWorkbenchContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 薪酬工作台必须明确当前周期，并给出最近一次工资单事实。 */
class PayrollWorkbenchContextTest {

    @Test
    void shouldProjectLatestSheetWhenSelectedPeriodHasNoSheet() {
        Employee boss = boss();
        PayrollAccessContext access = access(boss);
        AttendanceSummaryRepository attendance = mock(AttendanceSummaryRepository.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        CommissionItemRepository commissions = mock(CommissionItemRepository.class);
        PayslipRepository payslips = mock(PayslipRepository.class);
        PayableRepository payables = mock(PayableRepository.class);

        PayrollSheet latest = new PayrollSheet();
        latest.setSheetId(9001L);
        latest.setPeriod("2027-01");
        latest.setStatus(PayrollSheetStatus.Paid);
        latest.setTotalNetAmount(new BigDecimal("5727.27"));

        when(attendance.findAll(any(Specification.class))).thenReturn(List.of());
        when(sheets.findAll(any(Specification.class))).thenReturn(List.of(latest));
        when(commissions.findAll(any(Specification.class))).thenReturn(List.of());
        when(payslips.findAll(any(Specification.class))).thenReturn(List.of());
        when(payables.findAll(any(Specification.class))).thenReturn(List.of());

        var context = new PayrollWorkbenchContext(access, attendance, sheets, commissions, payslips, payables);
        var report = context.report(boss, "2026-10");

        assertThat(report.getCurrentPeriodHasSheet()).isFalse();
        assertThat(report.getLatestSheetId()).isEqualTo(9001L);
        assertThat(report.getLatestSheetPeriod()).isEqualTo("2027-01");
        assertThat(report.getLatestSheetStatus()).isEqualTo(PayrollSheetStatus.Paid);
        assertThat(report.getLatestSheetTotalNetAmount()).isEqualByComparingTo("5727.27");
    }

    @Test
    void shouldFilterSalaryPayableBySelectedPeriodButKeepCommissionPayable() {
        Employee boss = boss();
        PayrollAccessContext access = access(boss);
        AttendanceSummaryRepository attendance = mock(AttendanceSummaryRepository.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        CommissionItemRepository commissions = mock(CommissionItemRepository.class);
        PayslipRepository payslips = mock(PayslipRepository.class);
        PayableRepository payables = mock(PayableRepository.class);

        when(attendance.findAll(any(Specification.class))).thenReturn(List.of());
        when(sheets.findAll(any(Specification.class))).thenReturn(List.of());
        when(commissions.findAll(any(Specification.class))).thenReturn(List.of());
        when(payslips.findAll(any(Specification.class))).thenReturn(List.of());
        when(payables.findAll(any(Specification.class))).thenReturn(List.of(
                payable(PayableType.EmployeeSalary, "2027-01", "100.00"),
                payable(PayableType.EmployeeSalary, "2026-10", "50.00"),
                payable(PayableType.EmployeeCommission, null, "80.00")));

        var context = new PayrollWorkbenchContext(access, attendance, sheets, commissions, payslips, payables);
        var report = context.report(boss, "2026-10");

        assertThat(report.getPendingEmployeePayableAmount()).isEqualByComparingTo("130.00");
    }

    private PayrollAccessContext access(Employee boss) {
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireManager(boss)).thenReturn(boss);
        when(access.visible(any(Employee.class), any(Class.class))).thenReturn(mock(Specification.class));
        return access;
    }

    private Payable payable(PayableType type, String period, String amount) {
        Payable payable = new Payable();
        payable.setType(type);
        payable.setPeriod(period);
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setStatus(PayableStatus.Pending);
        return payable;
    }

    private Employee boss() {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setRole(EmployeeRole.Boss);
        return employee;
    }
}
