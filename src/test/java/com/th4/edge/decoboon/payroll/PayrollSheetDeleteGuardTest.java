package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.service.sheets.delete.support.Delete;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollSheetDeleteGuardTest {

    @Test
    void shouldDeleteDraftWithoutFinancialFacts() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, company);
        PayrollSheet sheet = sheet(company, PayrollSheetStatus.Draft);

        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireDeleteActor(boss)).thenReturn(boss);
        when(access.visibleSheets(boss)).thenReturn(mock(org.springframework.data.jpa.domain.Specification.class));
        when(sheets.findOne(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(Optional.of(sheet));
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L)).thenReturn(List.of());

        Delete delete = new Delete(sheets, items, attendances, payables, access, new PayrollAttendanceAuditContext());
        ReflectionTestUtils.setField(delete, "employee", boss);
        ReflectionTestUtils.setField(delete, "sheetId", 10L);

        assertThat(delete.call(new PayrollSheet())).isSameAs(sheet);
        verify(sheets).delete(sheet);
    }

    @Test
    void shouldRejectNonDraft() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, company);
        PayrollSheet sheet = sheet(company, PayrollSheetStatus.Pending);

        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireDeleteActor(boss)).thenReturn(boss);
        when(access.visibleSheets(boss)).thenReturn(mock(org.springframework.data.jpa.domain.Specification.class));
        when(sheets.findOne(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(Optional.of(sheet));

        Delete delete = new Delete(sheets, items, attendances, payables, access, new PayrollAttendanceAuditContext());
        ReflectionTestUtils.setField(delete, "employee", boss);
        ReflectionTestUtils.setField(delete, "sheetId", 10L);

        assertThatThrownBy(() -> delete.call(new PayrollSheet()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅草稿");
        verify(sheets, never()).delete(sheet);
    }

    @Test
    void shouldRestoreAttendanceToConfirmedWhenDraftIsDeleted() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, company);
        Employee staff = employee(2L, company);
        PayrollSheet sheet = sheet(company, PayrollSheetStatus.Draft);
        PayrollItem item = new PayrollItem();
        item.setItemId(20L);
        item.setCompany(company);
        item.setEmployee(staff);
        item.setSheet(sheet);
        item.setStatus(PayrollItemStatus.Draft);
        AttendanceSummary attendance = new AttendanceSummary();
        attendance.setCompany(company);
        attendance.setEmployee(staff);
        attendance.setPayrollMonth(sheet.getPeriod());
        attendance.setStatus(AttendanceStatus.InPayroll);

        PayrollSheetRepository sheets = mock(PayrollSheetRepository.class);
        PayrollItemRepository items = mock(PayrollItemRepository.class);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireDeleteActor(boss)).thenReturn(boss);
        when(access.visibleSheets(boss)).thenReturn(mock(org.springframework.data.jpa.domain.Specification.class));
        when(sheets.findOne(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(Optional.of(sheet));
        when(items.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(100L, 10L)).thenReturn(List.of(item));
        when(attendances.findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(100L, "2026-08"))
                .thenReturn(List.of(attendance));

        Delete delete = new Delete(sheets, items, attendances, payables, access, new PayrollAttendanceAuditContext());
        ReflectionTestUtils.setField(delete, "employee", boss);
        ReflectionTestUtils.setField(delete, "sheetId", 10L);

        delete.call(new PayrollSheet());

        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.Confirmed);
        verify(attendances).saveAll(List.of(attendance));
        verify(items).deleteAll(List.of(item));
        verify(sheets).delete(sheet);
    }

    private PayrollSheet sheet(Company company, PayrollSheetStatus status) {
        PayrollSheet sheet = new PayrollSheet();
        sheet.setSheetId(10L);
        sheet.setCompany(company);
        sheet.setPeriod("2026-08");
        sheet.setStatus(status);
        return sheet;
    }

    private Employee employee(Long empId, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(EmployeeRole.Boss);
        employee.setCompany(company);
        return employee;
    }
}
