package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.service.attendances.summary.save.support.Save;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollAttendanceScheduledDaysTest {

    @Test
    void shouldSnapshotScheduledDaysFromActiveCompensationScheme() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setStatus(CompensationStatus.Active);
        scheme.setScheduledDays(24);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        CompensationSchemeRepository compensations = mock(CompensationSchemeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Save provider = new Save(attendances, compensations, access);
        ReflectionTestUtils.setField(provider, "employee", boss);
        ReflectionTestUtils.setField(provider, "empId", target.getEmpId());
        ReflectionTestUtils.setField(provider, "month", "2026-10");

        when(access.requireEditor(boss)).thenReturn(boss);
        when(access.resolveEmployee(boss, target.getEmpId())).thenReturn(target);
        doNothing().when(access).applyOwnership(any(AttendanceSummary.class), eq(boss), eq(target));
        when(attendances.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                company.getComId(), target.getEmpId(), "2026-10")).thenReturn(Optional.empty());
        when(compensations.findByCompany_ComIdAndEmployee_EmpIdAndStatus(
                company.getComId(), target.getEmpId(), CompensationStatus.Active)).thenReturn(Optional.of(scheme));
        when(attendances.save(any(AttendanceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceSummary saved = provider.call(new AttendanceSummary());

        assertThat(saved.getScheduledDays()).isEqualTo(24);
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
