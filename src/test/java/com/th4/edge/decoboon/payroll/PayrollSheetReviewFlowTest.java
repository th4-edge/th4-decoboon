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
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-093 / DEC-097：财务可选复核，BOSS/本店店长可退回，驳回原因必填。 */
class PayrollSheetReviewFlowTest {

    @Test
    void financeCanSubmitDraftForReview() {
        Fixture f = fixture(PayrollSheetStatus.Draft);
        when(f.access.requireEditor(f.finance)).thenReturn(f.finance);
        when(f.access.visibleSheets(f.finance)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));
        when(f.sheets.save(any(PayrollSheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(f.items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L))
                .thenReturn(List.of(f.item));
        when(f.attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(100L, "2026-09"))
                .thenReturn(List.of(f.attendance));

        PayrollSheet result = f.context.submitReview(f.finance, 10L);

        assertThat(result.getStatus()).isEqualTo(PayrollSheetStatus.Pending);
        verify(f.access).requireStoreItemsInStore(f.finance, List.of(f.item));
        verify(f.audit).record(f.finance, result, "SUBMIT_REVIEW",
                PayrollSheetStatus.Draft, PayrollSheetStatus.Pending, null);
    }

    @Test
    void shouldRejectSubmitReviewWhenSheetContainsOtherStoreItems() {
        Fixture f = fixture(PayrollSheetStatus.Draft);
        when(f.access.requireEditor(f.finance)).thenReturn(f.finance);
        when(f.access.visibleSheets(f.finance)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));
        when(f.items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L))
                .thenReturn(List.of(f.item));
        doThrow(new DefinedException(new com.th4.edge.framework.exception.DefaultErrorCode(
                "403", "工资单包含其他门店工资项，不能由当前门店账号整单操作")))
                .when(f.access).requireStoreItemsInStore(f.finance, List.of(f.item));

        assertThatThrownBy(() -> f.context.submitReview(f.finance, 10L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("其他门店工资项");
    }

    @Test
    void bossCanReturnPendingWithReason() {
        Fixture f = fixture(PayrollSheetStatus.Pending);
        when(f.access.requireReturnActor(f.boss)).thenReturn(f.boss);
        when(f.access.visibleSheets(f.boss)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));
        when(f.sheets.save(any(PayrollSheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollSheet result = f.context.returnToDraft(f.boss, 10L, "考勤数据需要修正");

        assertThat(result.getStatus()).isEqualTo(PayrollSheetStatus.Draft);
        assertThat(result.getRemark()).isEqualTo("退回原因：考勤数据需要修正");
        verify(f.audit).record(f.boss, result, "RETURN_TO_DRAFT",
                PayrollSheetStatus.Pending, PayrollSheetStatus.Draft, "考勤数据需要修正");
    }

    @Test
    void draftCannotBeConfirmedBeforeFinanceSubmitsReview() {
        Fixture f = fixture(PayrollSheetStatus.Draft);
        when(f.access.requireConfirmActor(f.boss)).thenReturn(f.boss);
        when(f.access.visibleSheets(f.boss)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));

        assertThatThrownBy(() -> f.context.confirm(f.boss, 10L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未提交复核");
        verify(f.items, org.mockito.Mockito.never())
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(any(), any());
    }

    @Test
    void pendingConfirmationRejectsAttendanceOutsidePayroll() {
        Fixture f = fixture(PayrollSheetStatus.Pending);
        f.attendance.setStatus(AttendanceStatus.Confirmed);
        when(f.access.requireConfirmActor(f.boss)).thenReturn(f.boss);
        when(f.access.visibleSheets(f.boss)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));
        when(f.items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L))
                .thenReturn(List.of(f.item));
        when(f.attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(100L, "2026-09"))
                .thenReturn(List.of(f.attendance));

        assertThatThrownBy(() -> f.context.confirm(f.boss, 10L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("考勤未进入工资单");
    }

    @Test
    void pendingConfirmationRejectsIncompleteAttendance() {
        Fixture f = fixture(PayrollSheetStatus.Pending);
        f.attendance.setNormalDays(new BigDecimal("23.00"));
        when(f.access.requireConfirmActor(f.boss)).thenReturn(f.boss);
        when(f.access.visibleSheets(f.boss)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));
        when(f.items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L))
                .thenReturn(List.of(f.item));
        when(f.attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(100L, "2026-09"))
                .thenReturn(List.of(f.attendance));

        assertThatThrownBy(() -> f.context.confirm(f.boss, 10L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("必须等于应出勤天数");
    }

    @Test
    void returnRequiresReason() {
        Fixture f = fixture(PayrollSheetStatus.Pending);
        when(f.access.requireReturnActor(f.boss)).thenReturn(f.boss);
        when(f.access.visibleSheets(f.boss)).thenReturn(mock(Specification.class));
        when(f.sheets.findOne(any(Specification.class))).thenReturn(Optional.of(f.sheet));

        assertThatThrownBy(() -> f.context.returnToDraft(f.boss, 10L, "  "))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("退回原因不能为空");
    }

    private Fixture fixture(PayrollSheetStatus status) {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee finance = employee(2L, EmployeeRole.Finance, company);
        Employee staff = employee(3L, EmployeeRole.Designer, company);
        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(10L);
        sheet.setCompany(company);
        sheet.setPeriod("2026-09");
        sheet.setStatus(status);
        sheet.setAttendanceComplete(Boolean.TRUE);
        PayrollItem item = new PayrollItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setEmployee(staff);
        item.setSheet(sheet);
        item.setStatus(PayrollItemStatus.Draft);
        item.setGrossAmount(new BigDecimal("5000.00"));
        item.setDeductionAmount(BigDecimal.ZERO);
        item.setNetAmount(new BigDecimal("5000.00"));
        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setCompany(company);
        attendance.setEmployee(staff);
        attendance.setPayrollMonth("2026-09");
        attendance.setScheduledDays(24);
        attendance.setNormalDays(new BigDecimal("24.00"));
        attendance.setStatus(AttendanceStatus.InPayroll);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollSettlementAuditContext audit = mock(PayrollSettlementAuditContext.class);
        PayrollSettlementContext context = new PayrollSettlementContext(
                access, sheets, items, attendances, new PayrollAttendanceValidationContext(),
                mock(PayslipRepository.class), mock(PayableRepository.class), audit);
        return new Fixture(boss, finance, sheet, item, attendance, access, sheets, items,
                attendances, context, audit);
    }

    private Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setName("T" + empId);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }

    private record Fixture(Employee boss, Employee finance, PayrollSheet sheet, PayrollItem item,
                           AttendanceSummary attendance, PayrollAccessContext access,
                           PayrollSheetRepository sheets, PayrollItemRepository items,
                           AttendanceSummaryRepository attendances,
                           PayrollSettlementContext context,
                           PayrollSettlementAuditContext audit) {}
}
