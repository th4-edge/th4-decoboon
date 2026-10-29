package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.service.attendances.summary.save.support.Save;
import com.th4.edge.decoboon.payroll.service.attendances.summary.update.support.Update;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollAttendanceScheduledDaysTest {

    @Test
    void shouldSnapshotScheduledDaysFromActiveCompensationScheme() {
        Save provider = saveProvider(24, null);
        AttendanceSummary saved = provider.call(new AttendanceSummary());

        assertThat(saved.getScheduledDays()).isEqualTo(24);
    }

    @Test
    void shouldUseSubmittedScheduledDaysInsteadOfSchemeDefault() {
        Save provider = saveProvider(24, 26);
        AttendanceSummary saved = provider.call(new AttendanceSummary());

        assertThat(saved.getScheduledDays()).isEqualTo(26);
    }

    @Test
    void shouldRejectScheduledDaysOutsideSupportedRange() {
        Save provider = saveProvider(24, 32);

        assertThatThrownBy(() -> provider.call(new AttendanceSummary()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo("400");
                    assertThat(error.getMessage()).contains("1 至 31");
                });
    }

    @Test
    void shouldUpdateScheduledDaysSnapshot() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setCompany(company);
        summary.setEmployee(target);
        summary.setPayrollMonth("2026-10");
        summary.setScheduledDays(24);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollItemRepository payrollItems = mock(PayrollItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Update provider = new Update(attendances, payrollItems, access);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "summaryId", 10L);
        ReflectionTestUtils.setField(provider, "scheduledDays", 27);

        when(access.requireManager(boss)).thenReturn(boss);
        when(attendances.findById(10L)).thenReturn(Optional.of(summary));
        when(attendances.save(any(AttendanceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceSummary saved = provider.call(new AttendanceSummary());

        assertThat(saved.getScheduledDays()).isEqualTo(27);
    }

    @Test
    void shouldRejectInvalidScheduledDaysOnUpdate() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setCompany(company);
        summary.setEmployee(target);
        summary.setPayrollMonth("2026-10");
        summary.setScheduledDays(24);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollItemRepository payrollItems = mock(PayrollItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Update provider = new Update(attendances, payrollItems, access);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "summaryId", 10L);
        ReflectionTestUtils.setField(provider, "scheduledDays", 0);

        when(access.requireManager(boss)).thenReturn(boss);
        when(attendances.findById(10L)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> provider.call(new AttendanceSummary()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo("400");
                    assertThat(error.getMessage()).contains("1 至 31");
                });
        verify(attendances, never()).save(any(AttendanceSummary.class));
    }

    private Save saveProvider(int schemeScheduledDays, Integer submittedScheduledDays) {
        Company company = new Company();
        company.setComId(100L);
        Employee manager = employee(1L, EmployeeRole.StoreManager, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setStatus(CompensationStatus.Active);
        scheme.setScheduledDays(schemeScheduledDays);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        CompensationSchemeRepository compensations = mock(CompensationSchemeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Save provider = new Save(attendances, compensations, access);
        ReflectionTestUtils.setField(provider, "employee", manager);
        ReflectionTestUtils.setField(provider, "empId", target.getEmpId());
        ReflectionTestUtils.setField(provider, "month", "2026-10");
        ReflectionTestUtils.setField(provider, "scheduledDays", submittedScheduledDays);

        when(access.requireManager(manager)).thenReturn(manager);
        when(access.resolveEmployee(manager, target.getEmpId())).thenReturn(target);
        doNothing().when(access).applyOwnership(any(AttendanceSummary.class), eq(manager), eq(target));
        when(attendances.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                company.getComId(), target.getEmpId(), "2026-10")).thenReturn(Optional.empty());
        when(compensations.findEffective(
                company.getComId(), target.getEmpId(), CompensationStatus.Active,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(List.of(scheme));
        when(attendances.save(any(AttendanceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return provider;
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
