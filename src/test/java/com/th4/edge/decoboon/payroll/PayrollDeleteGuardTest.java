package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.service.attendances.summary.delete.support.Delete;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollDeleteGuardTest {

    @Test
    void shouldRejectCompensationDeleteWhenPayrollFactsExist() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        CompensationScheme scheme = new CompensationScheme();
        scheme.setSchemeId(10L);
        scheme.setEmployee(target);

        CompensationSchemeRepository schemes = mock(CompensationSchemeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        PayrollCompensationContext compensationContext = mock(PayrollCompensationContext.class);
        PayrollCompensationAuditContext audit = mock(PayrollCompensationAuditContext.class);
        var delete = new com.th4.edge.decoboon.payroll.service.compensations.delete.support.Delete(
                schemes, access, compensationContext, audit);
        ReflectionTestUtils.setField(delete, "employee", boss);
        ReflectionTestUtils.setField(delete, "schemeId", 10L);

        when(access.requireEditor(boss)).thenReturn(boss);
        when(access.visible(boss, CompensationScheme.class)).thenReturn(mock(Specification.class));
        when(schemes.findOne(any(Specification.class))).thenReturn(Optional.of(scheme));
        org.mockito.Mockito.doThrow(new com.th4.edge.framework.exception.DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode("409", "已形成工资事实，不能删除薪酬方案")))
                .when(compensationContext).requireUnlocked(company.getComId(), target);

        assertThatThrownBy(() -> delete.call(new CompensationScheme()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("不能删除薪酬方案");
    }

    @Test
    void shouldRejectAttendanceDeleteWhenAlreadyInPayroll() {
        Company company = company();
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setSummaryId(20L);
        summary.setEmployee(target);
        summary.setPayrollMonth("2026-10");
        summary.setStatus(AttendanceStatus.InPayroll);

        AttendanceSummaryRepository attendances = mock(AttendanceSummaryRepository.class);
        PayrollItemRepository payrollItems = mock(PayrollItemRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        Delete delete = new Delete(attendances, payrollItems, access);
        ReflectionTestUtils.setField(delete, "employee", boss);
        ReflectionTestUtils.setField(delete, "summaryId", 20L);

        when(access.requireEditor(boss)).thenReturn(boss);
        when(access.visible(boss, AttendanceSummary.class)).thenReturn(mock(Specification.class));
        when(attendances.findOne(any(Specification.class))).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> delete.call(new AttendanceSummary()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("不能删除");
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
        employee.setCompany(company);
        return employee;
    }
}
