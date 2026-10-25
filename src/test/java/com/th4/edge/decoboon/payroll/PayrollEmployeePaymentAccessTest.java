package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PayableSettlementRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.payroll.context.EmployeePaymentContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-093：店长不得发放员工工资或提成。 */
class PayrollEmployeePaymentAccessTest {

    @Test
    void storeManagerCannotPayEmployeePayables() {
        Company company = new Company();
        company.setComId(100L);
        Employee manager = new Employee();
        manager.setEmpId(1L);
        manager.setRole(EmployeeRole.StoreManager);
        manager.setCompany(company);

        PayrollAccessContext payrollAccess = mock(PayrollAccessContext.class);
        when(payrollAccess.requireEditor(manager)).thenThrow(
                new DefinedException(new DefaultErrorCode("403", "无权执行该操作")));
        PaymentRepository payments = mock(PaymentRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        EmployeePaymentContext context = new EmployeePaymentContext(
                mock(FinanceAccessContext.class),
                payrollAccess,
                mock(FinanceIdempotencyContext.class),
                payments,
                payables,
                mock(PayableSettlementRepository.class),
                mock(CommissionItemRepository.class),
                mock(PayrollItemRepository.class),
                mock(PayrollSheetRepository.class),
                mock(PayslipRepository.class));

        assertThatThrownBy(() -> context.pay(manager, List.of(1L), new BigDecimal("1.00"),
                FundAccount.Cash, LocalDate.of(2026, 10, 23), null, "b3b1-role", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权");

        verify(payrollAccess).requireEditor(manager);
        verify(payments, never()).save(any());
        verify(payables, never()).save(any());
    }
}
