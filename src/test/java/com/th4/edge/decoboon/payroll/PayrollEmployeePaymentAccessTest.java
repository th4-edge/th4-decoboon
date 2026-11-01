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

/** DEC-097：本店店长可进入员工发放动作，后续仍按应付事实校验。 */
class PayrollEmployeePaymentAccessTest {

    @Test
    void storeManagerUsesPaymentActorAndStillRequiresPayableFacts() {
        Company company = new Company();
        company.setComId(100L);
        Employee manager = new Employee();
        manager.setEmpId(1L);
        manager.setRole(EmployeeRole.StoreManager);
        manager.setCompany(company);

        PayrollAccessContext payrollAccess = mock(PayrollAccessContext.class);
        when(payrollAccess.requirePaymentActor(manager)).thenReturn(manager);
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

        assertThatThrownBy(() -> context.pay(manager, List.of(), new BigDecimal("1.00"),
                FundAccount.Cash, LocalDate.of(2026, 10, 23), null, "b3b1-role", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("员工应付单");

        verify(payrollAccess).requirePaymentActor(manager);
        verify(payments, never()).save(any());
        verify(payables, never()).save(any());
    }
}
