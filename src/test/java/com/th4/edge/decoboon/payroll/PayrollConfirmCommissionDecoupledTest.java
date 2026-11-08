package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceValidationContext;
import com.th4.edge.decoboon.payroll.context.PayrollSettlementAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollSettlementContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-047 / BR-039：工资单确认不得以提成结清为前置。 */
class PayrollConfirmCommissionDecoupledTest {

    @Test
    void shouldConfirmSheetEvenWhenCommissionIncomplete() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee staff = employee(2L, EmployeeRole.Designer, company);

        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(10L);
        sheet.setCompany(company);
        sheet.setPeriod("2026-08");
        sheet.setStatus(PayrollSheetStatus.Pending);
        sheet.setAttendanceComplete(Boolean.TRUE);
        // 关键：提成未结清（3 笔 Accrued），但工资单仍必须可以确认。
        sheet.setCommissionComplete(Boolean.FALSE);

        PayrollItem item = new PayrollItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setEmployee(staff);
        item.setSheet(sheet);
        item.setStatus(PayrollItemStatus.Draft);
        item.setGrossAmount(new BigDecimal("6000.00"));
        item.setDeductionAmount(new BigDecimal("0.00"));
        item.setNetAmount(new BigDecimal("6000.00"));

        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setCompany(company);
        attendance.setEmployee(staff);
        attendance.setPayrollMonth("2026-08");
        attendance.setScheduledDays(30);
        attendance.setNormalDays(new BigDecimal("30.00"));
        attendance.setStatus(AttendanceStatus.InPayroll);

        PayrollAccessContext access = mock(PayrollAccessContext.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayslipRepository payslips = mock(PayslipRepository.class);
        PayableRepository payables = mock(PayableRepository.class);

        when(access.requireConfirmActor(boss)).thenReturn(boss);
        when(access.visibleSheets(boss)).thenReturn(mock(Specification.class));
        when(sheets.findOne(any(Specification.class))).thenReturn(Optional.of(sheet));
        when(sheets.save(any(PayrollSheet.class))).thenAnswer(i -> i.getArgument(0));
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(any(), any()))
                .thenReturn(List.of(item));
        when(attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(any(), any()))
                .thenReturn(List.of(attendance));
        when(items.save(any(PayrollItem.class))).thenAnswer(i -> i.getArgument(0));
        when(payables.findByCompany_ComIdAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(payables.save(any())).thenAnswer(i -> i.getArgument(0));
        when(payslips.findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(payslips.save(any())).thenAnswer(i -> i.getArgument(0));

        PayrollSettlementContext context =
                new PayrollSettlementContext(access, sheets, items, attendances,
                        new PayrollAttendanceValidationContext(), payslips, payables,
                        mock(PayrollSettlementAuditContext.class));

        PayrollSheet result = context.confirm(boss, 10L);

        assertThat(result.getStatus()).isEqualTo(PayrollSheetStatus.Confirmed);
        assertThat(result.getCommissionComplete()).isFalse();
    }

    private Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setName("T" + empId);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
