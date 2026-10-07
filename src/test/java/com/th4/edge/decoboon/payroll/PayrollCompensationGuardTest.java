package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 薪酬方案响应必须收窄员工图，并在入库前明确拦截同一生效日重复。 */
class PayrollCompensationGuardTest {

    @Test
    void compensationProvidersMustUseBoundedEmployeeProjection() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.payroll.service.compensations.page.support.Page.class,
                com.th4.edge.decoboon.payroll.service.compensations.save.support.Save.class,
                com.th4.edge.decoboon.payroll.service.compensations.info.support.Info.class,
                com.th4.edge.decoboon.payroll.service.compensations.update.support.Update.class)) {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getSimpleName() + " 必须声明 @Properties").isNotNull();

            AllowProperties employeeAllow = allowFor(properties, Employee.class);
            assertThat(Arrays.asList(employeeAllow.properties()))
                    .contains("empId", "name", "avatarUrl", "role", "status", "store", "team")
                    .doesNotContain("telNo", "invite", "company", "resignReason");

            assertThat(Arrays.asList(allowFor(properties, Store.class).properties()))
                    .containsExactly("storeId", "name");
            assertThat(Arrays.asList(allowFor(properties, Team.class).properties()))
                    .containsExactly("teamId", "name");
        }
    }

    @Test
    void otherPayrollResponsesMustNotExposeFullNestedGraphs() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.payroll.service.attendances.summary.page.support.Page.class,
                com.th4.edge.decoboon.payroll.service.attendances.summary.info.support.Info.class,
                com.th4.edge.decoboon.payroll.service.attendances.summary.save.support.Save.class,
                com.th4.edge.decoboon.payroll.service.attendances.summary.update.support.Update.class,
                com.th4.edge.decoboon.payroll.service.attendances.summary.delete.support.Delete.class,
                com.th4.edge.decoboon.payroll.service.compensations.delete.support.Delete.class)) {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getSimpleName() + " 必须声明 @Properties").isNotNull();
            assertThat(Arrays.asList(allowFor(properties, Employee.class).properties()))
                    .contains("empId", "name", "avatarUrl", "role", "status", "store", "team")
                    .doesNotContain("telNo", "invite", "company", "resignReason");
            assertThat(Arrays.asList(allowFor(properties, Store.class).properties()))
                    .containsExactly("storeId", "name");
            assertThat(Arrays.asList(allowFor(properties, Team.class).properties()))
                    .containsExactly("teamId", "name");
        }

        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.payroll.service.sheets.build.support.Build.class,
                com.th4.edge.decoboon.payroll.service.sheets.info.support.Info.class,
                com.th4.edge.decoboon.payroll.service.sheets.update.support.Update.class)) {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getSimpleName() + " 必须声明 @Properties").isNotNull();
            assertThat(Arrays.asList(allowFor(
                    properties, com.th4.edge.decoboon.payroll.model.PayrollSheet.class).properties()))
                    .contains("sheetId", "period", "status", "confirmedBy");
            assertThat(Arrays.asList(allowFor(properties, Employee.class).properties()))
                    .containsExactly("empId", "name", "role");
        }

        Properties storePayrollInfo = com.th4.edge.decoboon.payroll.service.storepayroll.info.support.Info.class
                .getAnnotation(Properties.class);
        assertThat(storePayrollInfo).isNotNull();
        assertThat(Arrays.asList(allowFor(storePayrollInfo, Employee.class).properties()))
                .containsExactly("empId", "name", "role");
        assertThat(Arrays.asList(allowFor(
                storePayrollInfo, com.th4.edge.decoboon.payroll.model.PayrollSheet.class).properties()))
                .containsExactly("sheetId", "period", "status");
        assertThat(Arrays.asList(allowFor(storePayrollInfo, com.th4.edge.decoboon.project.model.Project.class).properties()))
                .containsExactly("projectId", "name", "no", "status");
    }

    @Test
    void shouldRejectDuplicateEffectiveDateBeforeInsert() {
        Company company = new Company();
        company.setComId(100L);
        Employee boss = employee(1L, EmployeeRole.Boss, company);
        Employee target = employee(2L, EmployeeRole.Designer, company);

        CompensationScheme existing = new CompensationScheme();
        existing.setEffectiveFrom(LocalDate.of(2026, 10, 7));

        CompensationSchemeRepository repository = mock(CompensationSchemeRepository.class);
        PayrollAccessContext access = mock(PayrollAccessContext.class);
        var save = new com.th4.edge.decoboon.payroll.service.compensations.save.support.Save(repository, access);
        ReflectionTestUtils.setField(save, "employee", boss);
        ReflectionTestUtils.setField(save, "empId", target.getEmpId());
        ReflectionTestUtils.setField(save, "baseSalary", new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(save, "scheduledDays", 26);
        ReflectionTestUtils.setField(save, "effectiveFrom", LocalDate.of(2026, 10, 7));

        when(access.requireEditor(boss)).thenReturn(boss);
        when(access.resolveEmployee(boss, target.getEmpId())).thenReturn(target);
        when(repository.findByCompany_ComIdAndEmployeeOrderByEffectiveFromDesc(company.getComId(), target))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> save.call(new CompensationScheme()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已有薪酬方案");
        verify(repository, never()).save(any(CompensationScheme.class));
    }

    private static AllowProperties allowFor(Properties properties, Class<?> model) {
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 " + model.getSimpleName() + " 响应白名单"));
    }

    private static Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
