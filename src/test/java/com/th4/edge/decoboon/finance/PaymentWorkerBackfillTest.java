package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.service.payments.update.support.Update;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工资付款补录工人只改关联，不改金额与付款事实。 */
class PaymentWorkerBackfillTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final WorkerRepository workerRepository = mock(WorkerRepository.class);
    private final FinanceAccessContext accessContext = mock(FinanceAccessContext.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);
    private final VoucherContext voucherContext = mock(VoucherContext.class);

    @Test
    void shouldOnlyAssociateWorkerAndRefreshVoucher() {
        Company company = company();
        Employee finance = employee(company, 1L);
        Payable payable = payable(company, PayableType.Salary);
        Payment payment = payment(company, payable);
        Worker worker = worker(company);
        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(dataScopeContext.resolve(finance)).thenReturn(new DataScope(DataScopeType.Company, 1L, null, null, 1L));
        when(paymentRepository.findOne(any(Specification.class))).thenReturn(Optional.of(payment));
        when(workerRepository.findById(9L)).thenReturn(Optional.of(worker));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = provider(finance, 8L, 9L).call(new Payment());

        assertThat(result.getWorker()).isSameAs(worker);
        assertThat(result.getAmount()).isEqualByComparingTo("300.00");
        assertThat(result.getAccount()).isEqualTo(FundAccount.CompanyBank);
        assertThat(result.getPaidAt()).isEqualTo(LocalDate.of(2026, 10, 6));
        verify(paymentRepository).save(payment);
        verify(voucherContext).rebuildForPayment(finance, 8L);
    }

    @Test
    void shouldRejectWorkerReplacement() {
        Company company = company();
        Employee finance = employee(company, 1L);
        Payment payment = payment(company, payable(company, PayableType.Salary));
        payment.setWorker(worker(company));
        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(dataScopeContext.resolve(finance)).thenReturn(new DataScope(DataScopeType.Company, 1L, null, null, 1L));
        when(paymentRepository.findOne(any(Specification.class))).thenReturn(Optional.of(payment));
        when(workerRepository.findById(10L)).thenReturn(Optional.of(worker(company)));

        assertThatThrownBy(() -> provider(finance, 8L, 10L).call(new Payment()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不允许替换");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(voucherContext, never()).rebuildForPayment(any(), any());
    }

    private Update provider(Employee finance, Long paymentId, Long workerId) {
        Update update = new Update(paymentRepository, workerRepository, accessContext, dataScopeContext, voucherContext);
        ReflectionTestUtils.setField(update, "employee", finance);
        ReflectionTestUtils.setField(update, "paymentId", paymentId);
        ReflectionTestUtils.setField(update, "workerId", workerId);
        return update;
    }

    private Company company() {
        Company company = new Company();
        company.setComId(1L);
        return company;
    }

    private Employee employee(Company company, Long empId) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setCompany(company);
        return employee;
    }

    private Payable payable(Company company, PayableType type) {
        Payable payable = new Payable();
        payable.setPayableId(2L);
        payable.setCompany(company);
        payable.setType(type);
        return payable;
    }

    private Payment payment(Company company, Payable payable) {
        Payment payment = new Payment();
        payment.setPaymentId(8L);
        payment.setCompany(company);
        payment.setPayable(payable);
        payment.setAmount(new BigDecimal("300.00"));
        payment.setAccount(FundAccount.CompanyBank);
        payment.setPaidAt(LocalDate.of(2026, 10, 6));
        return payment;
    }

    private Worker worker(Company company) {
        Worker worker = new Worker();
        worker.setWorkerId(9L);
        worker.setCompany(company);
        worker.setName("张师傅");
        return worker;
    }
}
