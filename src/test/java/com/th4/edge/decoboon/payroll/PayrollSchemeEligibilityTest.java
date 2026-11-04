package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
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
import java.time.LocalDateTime;
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

/**
 * 工资归属月份的员工范围以“薪酬方案生效期”判定，不以入职/离职时间判定。
 * 对应 DEC-045 / BR-015：生产麦鸿 2026-08 全员入职在 9 月但 8 月有生效方案，必须正常生成工资项。
 */
class PayrollSchemeEligibilityTest {

    private static final Long COM_ID = 100L;
    private static final String PERIOD = "2026-08";
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 8, 31);

    @Test
    void shouldIncludeEmployeeJoinedAfterPeriodWhenSchemeEffectiveInPeriod() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee lateJoiner = employee(2L, EmployeeRole.Designer, company);
        // 入职日期在工资月份之后，但 8 月有生效薪酬方案。
        lateJoiner.setJoinedAt(LocalDateTime.of(2026, 9, 24, 11, 43, 16));
        CompensationScheme scheme = scheme(new BigDecimal("3000.00"), 27);

        Fixture fixture = fixture(company, boss, lateJoiner, scheme, true);
        fixture.context.build(boss, PERIOD);

        ArgumentCaptor<PayrollItem> captor = ArgumentCaptor.forClass(PayrollItem.class);
        verify(fixture.items).save(captor.capture());
        assertThat(captor.getValue().getBaseSalaryAmount()).isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldExcludeEmployeeWithoutEffectiveSchemeInPeriod() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee unconfigured = employee(2L, EmployeeRole.Boss, company);
        unconfigured.setJoinedAt(LocalDateTime.of(2026, 9, 22, 16, 36, 31));

        Fixture fixture = fixture(company, boss, unconfigured, null, false);
        fixture.context.build(boss, PERIOD);

        verify(fixture.items, never()).save(any(PayrollItem.class));
    }

    @Test
    void shouldRejectBuildWhenEligibleEmployeeAttendanceIsMissing() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = scheme(new BigDecimal("3000.00"), 27);
        Fixture fixture = fixture(company, boss, target, scheme, true);
        when(fixture.attendances.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                company.getComId(), target.getEmpId(), PERIOD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixture.context.build(boss, PERIOD))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("考勤未确认");
        verify(fixture.items, never()).save(any(PayrollItem.class));
    }

    private Fixture fixture(Company company, Employee boss, Employee target,
                            CompensationScheme scheme, boolean hasScheme) {
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

        if (hasScheme) {
            when(compensations.findEffective(company.getComId(), target.getEmpId(),
                    CompensationStatus.Active, PERIOD_START, PERIOD_END)).thenReturn(List.of(scheme));
            when(attendances.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                    company.getComId(), target.getEmpId(), PERIOD)).thenReturn(Optional.of(attendance(target)));
            when(commissions.existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(any(), any(), anyList())).thenReturn(false);
            when(adjustments.findByCompany_ComIdAndEmployee_EmpIdAndStatusOrderByOccurredAtAscAdjustmentIdAsc(
                    any(), any(), any())).thenReturn(List.of());
            when(adjustmentContext.apply(anyList(), any())).thenReturn(BigDecimal.ZERO.setScale(2));
            doNothing().when(adjustmentContext).linkToPayrollItem(any(), anyList(), any());
            when(items.findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpIdAndAttributionKey(
                    any(), any(), eq(target.getEmpId()), eq("COMPANY"))).thenReturn(Optional.empty());
            when(items.save(any(PayrollItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        } else {
            when(compensations.findEffective(company.getComId(), target.getEmpId(),
                    CompensationStatus.Active, PERIOD_START, PERIOD_END)).thenReturn(List.of());
        }

        when(sheets.findByCompany_ComIdAndPeriod(company.getComId(), PERIOD)).thenReturn(Optional.empty());
        when(sheets.save(any(PayrollSheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(any(), any())).thenReturn(List.of());

        PayrollBuildContext context = new PayrollBuildContext(
                access, new PayrollCalculationContext(), employees, compensations, commissions,
                adjustments, adjustmentContext, attendances, sheets, items, new PayrollAttendanceAuditContext());
        return new Fixture(context, items, attendances);
    }

    private AttendanceSummary attendance(Employee employee) {
        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setEmployee(employee);
        attendance.setPayrollMonth(PERIOD);
        attendance.setScheduledDays(26);
        attendance.setStatus(AttendanceStatus.Confirmed);
        return attendance;
    }

    private Company company() {
        Company company = new Company();
        company.setComId(COM_ID);
        return company;
    }

    private Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }

    private CompensationScheme scheme(BigDecimal baseSalary, Integer scheduledDays) {
        CompensationScheme scheme = new CompensationScheme();
        scheme.setBaseSalary(baseSalary);
        scheme.setScheduledDays(scheduledDays);
        scheme.setEffectiveFrom(LocalDate.of(2026, 7, 15));
        scheme.setStatus(CompensationStatus.Active);
        return scheme;
    }

    private record Fixture(PayrollBuildContext context, PayrollItemRepository items,
                           AttendanceSummaryRepository attendances) {
    }
}
