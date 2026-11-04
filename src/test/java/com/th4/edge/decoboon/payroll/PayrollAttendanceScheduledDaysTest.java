package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceValidationContext;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void shouldRejectInvalidMonthBeforeDateParsing() {
        SaveFixture fixture = saveFixture(24, null);
        ReflectionTestUtils.setField(fixture.provider(), "month", "2026-13");
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo("400");
                    assertThat(error.getMessage()).contains("YYYY-MM");
                });
        verify(fixture.attendances(), never()).saveAndFlush(any(AttendanceSummary.class));
    }

    @Test
    void shouldRejectDayTotalAboveScheduledDaysOnSave() {
        SaveFixture fixture = saveFixture(24, 24);
        ReflectionTestUtils.setField(fixture.provider(), "normalDays", new BigDecimal("24"));
        ReflectionTestUtils.setField(fixture.provider(), "personalLeaveDays", new BigDecimal("1"));
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("合计不能超过应出勤天数");
        verify(fixture.attendances(), never()).saveAndFlush(any(AttendanceSummary.class));
    }

    @Test
    void shouldUpdateScheduledDaysSnapshot() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 24, 27, null, 0, 0);
        AttendanceSummary saved = fixture.provider().call(new AttendanceSummary());
        assertThat(saved.getScheduledDays()).isEqualTo(27);
        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Pending);
    }

    @Test
    void shouldRejectInvalidScheduledDaysOnUpdate() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 24, 0, null, 0, 0);
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo("400");
                    assertThat(error.getMessage()).contains("1 至 31");
                });
        verify(fixture.attendances(), never()).saveAndFlush(any(AttendanceSummary.class));
    }

    @Test
    void shouldRejectNegativeAttendanceDays() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 27, 27, null, -1, 0);
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("0 至 31");
    }

    @Test
    void shouldRejectClientStatusOtherThanConfirmation() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 27, 27, "InPayroll", 27, 0);
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo("400");
                    assertThat(error.getMessage()).contains("仅支持确认动作");
                });
    }

    @Test
    void shouldResetConfirmedAttendanceToPendingWhenDataChanges() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Confirmed, 27, 27, null, 27, 0);
        Employee confirmer = employee(99L, EmployeeRole.Boss, fixture.company());
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        fixture.summary().setConfirmedBy(confirmer);
        fixture.summary().setConfirmedAt(confirmedAt);
        ReflectionTestUtils.setField(fixture.provider(), "normalDays", new BigDecimal("26"));
        ReflectionTestUtils.setField(fixture.provider(), "personalLeaveDays", new BigDecimal("1"));

        AttendanceSummary saved = fixture.provider().call(new AttendanceSummary());

        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Pending);
        assertThat(saved.getConfirmedBy()).isNull();
        assertThat(saved.getConfirmedAt()).isNull();
    }

    @Test
    void shouldRequireCompleteDaysWhenConfirming() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 27, 27, "Confirmed", 22, 1);
        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("合计必须等于应出勤天数");
    }

    @Test
    void shouldConfirmCompleteAttendance() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 27, 27, "Confirmed", 26, 1);
        AttendanceSummary saved = fixture.provider().call(new AttendanceSummary());
        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Confirmed);
        assertThat(saved.getConfirmedBy()).isSameAs(fixture.boss());
        assertThat(saved.getConfirmedAt()).isNotNull();
    }

    @Test
    void shouldConfirmHalfDayAttendanceAndPreserveDecimalValues() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 22, 22, "Confirmed", 21, 0);
        ReflectionTestUtils.setField(fixture.provider(), "normalDays", new BigDecimal("21.5"));
        ReflectionTestUtils.setField(fixture.provider(), "personalLeaveDays", new BigDecimal("0.5"));

        AttendanceSummary saved = fixture.provider().call(new AttendanceSummary());

        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Confirmed);
        assertThat(saved.getNormalDays()).isEqualByComparingTo("21.5");
        assertThat(saved.getPersonalLeaveDays()).isEqualByComparingTo("0.5");
    }

    @Test
    void shouldRejectAttendanceDaysWithMoreThanTwoDecimalPlaces() {
        UpdateFixture fixture = updateFixture(AttendanceStatus.Pending, 22, 22, null, 21, 0);
        ReflectionTestUtils.setField(fixture.provider(), "personalLeaveDays", new BigDecimal("0.555"));

        assertThatThrownBy(() -> fixture.provider().call(new AttendanceSummary()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("最多保留两位小数");
    }

    @Test
    void shouldUseScopedLookupForUpdate() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        @SuppressWarnings("unchecked")
        Specification<AttendanceSummary> visible = mock(Specification.class);
        when(access.requireManager(boss)).thenReturn(boss);
        when(access.visible(boss, AttendanceSummary.class)).thenReturn(visible);
        when(attendances.findOne(any(Specification.class))).thenReturn(Optional.empty());
        Update provider = new Update(attendances, mock(PayrollItemRepository.class), access,
                new PayrollAttendanceValidationContext(), new PayrollAttendanceAuditContext());
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "summaryId", 10L);

        assertThatThrownBy(() -> provider.call(new AttendanceSummary()))
                .hasMessageContaining("考勤不存在或无权访问");
        verify(attendances).findOne(any(Specification.class));
        verify(attendances, never()).findById(any());
    }

    private Save saveProvider(int schemeScheduledDays, Integer submittedScheduledDays) {
        return saveFixture(schemeScheduledDays, submittedScheduledDays).provider();
    }

    private SaveFixture saveFixture(int schemeScheduledDays, Integer submittedScheduledDays) {
        Company company = company();
        Employee manager = employee(1L, EmployeeRole.StoreManager, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setStatus(CompensationStatus.Active);
        scheme.setScheduledDays(schemeScheduledDays);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        CompensationSchemeRepository compensations = mock(CompensationSchemeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Save provider = new Save(attendances, compensations, access,
                new PayrollAttendanceValidationContext(), new PayrollAttendanceAuditContext());
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
        when(attendances.saveAndFlush(any(AttendanceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new SaveFixture(provider, attendances);
    }

    private UpdateFixture updateFixture(AttendanceStatus currentStatus, int scheduledDays,
                                        Integer submittedScheduledDays, String status,
                                        int normalDays, int personalLeaveDays) {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setSummaryId(10L);
        summary.setCompany(company);
        summary.setEmployee(target);
        summary.setPayrollMonth("2026-10");
        summary.setScheduledDays(scheduledDays);
        summary.setNormalDays(BigDecimal.valueOf(normalDays));
        summary.setPersonalLeaveDays(BigDecimal.valueOf(personalLeaveDays));
        summary.setStatus(currentStatus);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollItemRepository payrollItems = mock(PayrollItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        @SuppressWarnings("unchecked")
        Specification<AttendanceSummary> visible = mock(Specification.class);
        when(access.requireManager(boss)).thenReturn(boss);
        when(access.visible(boss, AttendanceSummary.class)).thenReturn(visible);
        when(attendances.findOne(any(Specification.class))).thenReturn(Optional.of(summary));
        when(attendances.saveAndFlush(any(AttendanceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Update provider = new Update(attendances, payrollItems, access,
                new PayrollAttendanceValidationContext(), new PayrollAttendanceAuditContext());
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "summaryId", 10L);
        ReflectionTestUtils.setField(provider, "scheduledDays", submittedScheduledDays);
        ReflectionTestUtils.setField(provider, "status", status);
        return new UpdateFixture(provider, attendances, summary, boss, company);
    }

    private Company company() {
        Company company = new Company();
        company.setComId(100L);
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

    private record SaveFixture(Save provider, AttendanceSummaryRepository attendances) {
    }

    private record UpdateFixture(Update provider,
                                 AttendanceSummaryRepository attendances,
                                 AttendanceSummary summary,
                                 Employee boss,
                                 Company company) {
    }
}
