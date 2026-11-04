package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.CommissionAdjustmentContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollBuildContext;
import com.th4.edge.decoboon.payroll.context.PayrollCalculationContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionAdjustmentRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollBuildContextScheduledDaysTest {

    @Test
    void shouldUseConfirmedAttendanceSnapshotForAttendanceDeduction() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setBaseSalary(new BigDecimal("6000.00"));
        scheme.setScheduledDays(26);
        scheme.setStatus(CompensationStatus.Active);
        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setScheduledDays(24);
        attendance.setPersonalLeaveDays(1);
        attendance.setStatus(AttendanceStatus.Confirmed);

        PayrollAccessContext access = mock(PayrollAccessContext.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CompensationSchemeRepository compensations = mock(CompensationSchemeRepository.class);
        CommissionItemRepository commissions = mock(CommissionItemRepository.class);
        CommissionAdjustmentRepository adjustments = mock(CommissionAdjustmentRepository.class);
        CommissionAdjustmentContext adjustmentContext = mock(CommissionAdjustmentContext.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);

        when(access.requireEditor(boss)).thenReturn(boss);
        doNothing().when(access).applyCompanyOwnership(any(PayrollSheet.class), eq(boss));
        doNothing().when(access).applyOwnership(any(PayrollItem.class), eq(boss), eq(target), any(PayrollSheet.class), isNull());
        when(employees.findByCompany_ComId(company.getComId())).thenReturn(List.of(target));
        when(compensations.findEffective(
                company.getComId(), target.getEmpId(), CompensationStatus.Active,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(List.of(scheme));
        when(attendances.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                company.getComId(), target.getEmpId(), "2026-10")).thenReturn(Optional.of(attendance));
        when(commissions.existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(adjustments.findByCompany_ComIdAndEmployee_EmpIdAndStatusOrderByOccurredAtAscAdjustmentIdAsc(
                company.getComId(), target.getEmpId(), com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus.Pending))
                .thenReturn(List.of());
        when(adjustmentContext.apply(anyList(), any())).thenReturn(BigDecimal.ZERO.setScale(2));
        doNothing().when(adjustmentContext).linkToPayrollItem(any(), anyList(), any());
        when(sheets.findByCompany_ComIdAndPeriod(company.getComId(), "2026-10")).thenReturn(Optional.empty());
        when(sheets.save(any(PayrollSheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpIdAndAttributionKey(
                any(), any(), eq(target.getEmpId()), eq("COMPANY"))).thenReturn(Optional.empty());
        when(items.save(any(PayrollItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(any(), any())).thenReturn(List.of());

        PayrollBuildContext context = new PayrollBuildContext(
                access, new PayrollCalculationContext(), employees, compensations, commissions,
                adjustments, adjustmentContext, attendances, sheets, items, new PayrollAttendanceAuditContext());
        context.build(boss, "2026-10");

        ArgumentCaptor<PayrollItem> captor = ArgumentCaptor.forClass(PayrollItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getAttendanceDeduction()).isEqualByComparingTo("250.00");
        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.InPayroll);
        verify(attendances).saveAll(List.of(attendance));
    }

    @Test
    void shouldRejectRebuildWhenSheetIsPendingReview() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        PayrollSheet pending = new PayrollSheet();
        pending.setSheetId(10L);
        pending.setCompany(company);
        pending.setPeriod("2026-10");
        pending.setStatus(PayrollSheetStatus.Pending);

        PayrollAccessContext access = mock(PayrollAccessContext.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        when(access.requireEditor(boss)).thenReturn(boss);
        when(sheets.findByCompany_ComIdAndPeriod(company.getComId(), "2026-10"))
                .thenReturn(Optional.of(pending));

        PayrollBuildContext context = new PayrollBuildContext(
                access, mock(PayrollCalculationContext.class), employees,
                mock(CompensationSchemeRepository.class), mock(CommissionItemRepository.class),
                mock(CommissionAdjustmentRepository.class), mock(CommissionAdjustmentContext.class),
                mock(AttendanceSummaryRepository.class), sheets, mock(PayrollItemRepository.class), new PayrollAttendanceAuditContext());

        assertThatThrownBy(() -> context.build(boss, "2026-10"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已提交复核");
        verify(employees, never()).findByCompany_ComId(company.getComId());
    }

    @Test
    void shouldRemoveStaleDraftItemAndRestoreAttendanceWhenEmployeeLeavesSchemeScope() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee former = employee(2L, EmployeeRole.Designer, company);
        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(10L);
        sheet.setCompany(company);
        sheet.setPeriod("2026-10");
        sheet.setStatus(PayrollSheetStatus.Draft);
        PayrollItem stale = new PayrollItem();
        stale.setItemId(20L);
        stale.setCompany(company);
        stale.setEmployee(former);
        stale.setSheet(sheet);
        stale.setStatus(PayrollItemStatus.Draft);
        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setSummaryId(30L);
        attendance.setCompany(company);
        attendance.setEmployee(former);
        attendance.setPayrollMonth("2026-10");
        attendance.setScheduledDays(27);
        attendance.setNormalDays(27);
        attendance.setStatus(AttendanceStatus.InPayroll);

        PayrollAccessContext access = mock(PayrollAccessContext.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CompensationSchemeRepository schemes = mock(CompensationSchemeRepository.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        CommissionAdjustmentContext adjustmentContext = mock(CommissionAdjustmentContext.class);
        when(access.requireEditor(boss)).thenReturn(boss);
        when(sheets.findByCompany_ComIdAndPeriod(100L, "2026-10")).thenReturn(Optional.of(sheet));
        when(sheets.save(any(PayrollSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employees.findByCompany_ComId(100L)).thenReturn(List.of(former));
        when(schemes.findEffective(100L, 2L, CompensationStatus.Active,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(List.of());
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L))
                .thenReturn(List.of(stale), List.of());
        when(attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(100L, "2026-10"))
                .thenReturn(List.of(attendance));

        PayrollBuildContext context = new PayrollBuildContext(
                access, new PayrollCalculationContext(), employees, schemes,
                mock(CommissionItemRepository.class), mock(CommissionAdjustmentRepository.class),
                adjustmentContext, attendances, sheets, items, new PayrollAttendanceAuditContext());
        context.build(boss, "2026-10");

        verify(adjustmentContext).resetForDraftItem(boss, 20L);
        verify(items).deleteAll(List.of(stale));
        verify(items).flush();
        verify(attendances).saveAll(List.of(attendance));
        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.Confirmed);
    }

    private Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }
}
