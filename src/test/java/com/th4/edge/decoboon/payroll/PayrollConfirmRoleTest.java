package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollSettlementContext;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-046 APP-PAY-03：工资单确认动作人仅 BOSS，财务不得确认。 */
class PayrollConfirmRoleTest {

    @Test
    void shouldGuardSheetConfirmWithBossRole() {
        Company company = new Company();
        company.setComId(100L);
        Employee finance = employee(9L, EmployeeRole.Finance, company);

        PayrollAccessContext access = mock(PayrollAccessContext.class);
        when(access.requireBoss(finance)).thenThrow(
                new DefinedException(new DefaultErrorCode("403", "无权执行该操作")));
        PayrollSettlementContext context = new PayrollSettlementContext(
                access,
                mock(PayrollSheetRepository.class),
                mock(PayrollItemRepository.class),
                mock(PayslipRepository.class),
                mock(PayableRepository.class));

        assertThatThrownBy(() -> context.confirm(finance, 1L))
                .isInstanceOf(DefinedException.class);

        verify(access).requireBoss(finance);
        verify(access, never()).requireEditor(any());
    }

    private Employee employee(Long empId, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setRole(role);
        employee.setCompany(company);
        return employee;
    }
}
