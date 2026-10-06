package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.context.ReceiptRefundContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.payroll.context.CommissionAdjustmentContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceiptRefundContextTest {

    @Test
    void shouldCreateRefundAndTriggerCommissionAdjustment() {
        ReceiptRefundRepository refundRepository = mock(ReceiptRefundRepository.class);
        ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
        FinanceAccessContext accessContext = mock(FinanceAccessContext.class);
        FinanceIdempotencyContext idempotencyContext = mock(FinanceIdempotencyContext.class);
        CommissionAdjustmentContext adjustmentContext = mock(CommissionAdjustmentContext.class);
        ReceiptRefundContext context = new ReceiptRefundContext(
                refundRepository, receiptRepository, accessContext, idempotencyContext, adjustmentContext);

        Company company = new Company();
        company.setComId(1L);
        Employee finance = new Employee();
        finance.setEmpId(9L);
        finance.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        Receipt receipt = new Receipt();
        receipt.setReceiptId(10L);
        receipt.setCompany(company);
        receipt.setProject(project);
        receipt.setAmount(new BigDecimal("100000.00"));

        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(accessContext.value(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.findById(10L)).thenReturn(Optional.of(receipt));
        when(idempotencyContext.amount(any())).thenReturn("10000");
        when(idempotencyContext.text(any())).thenReturn("客户退款");
        when(idempotencyContext.fingerprint(any())).thenReturn("refund-fingerprint");
        when(idempotencyContext.clientKeyOr(any(), any())).thenReturn("refund-key");
        when(refundRepository.findByCompany_ComIdAndIdempotencyKey(1L, "refund-key")).thenReturn(Optional.empty());
        when(refundRepository.findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(1L, 10L))
                .thenReturn(List.of());
        when(refundRepository.saveAndFlush(any(ReceiptRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReceiptRefund result = context.create(finance, 10L, new BigDecimal("10000.00"),
                null, LocalDate.of(2026, 10, 7), "客户退款", null);

        assertThat(result.getAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getIdempotencyKey()).isEqualTo("refund-key");
        ArgumentCaptor<ReceiptRefund> captor = ArgumentCaptor.forClass(ReceiptRefund.class);
        verify(adjustmentContext).applyRefund(eq(finance), captor.capture());
        assertThat(captor.getValue()).isSameAs(result);
        verify(accessContext).recordLedger(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectRefundAboveReceiptBalance() {
        ReceiptRefundRepository refundRepository = mock(ReceiptRefundRepository.class);
        ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
        FinanceAccessContext accessContext = mock(FinanceAccessContext.class);
        FinanceIdempotencyContext idempotencyContext = mock(FinanceIdempotencyContext.class);
        ReceiptRefundContext context = new ReceiptRefundContext(
                refundRepository, receiptRepository, accessContext, idempotencyContext,
                mock(CommissionAdjustmentContext.class));

        Company company = new Company();
        company.setComId(1L);
        Employee finance = new Employee();
        finance.setEmpId(9L);
        finance.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        Receipt receipt = new Receipt();
        receipt.setReceiptId(10L);
        receipt.setCompany(company);
        receipt.setProject(project);
        receipt.setAmount(new BigDecimal("100000.00"));
        ReceiptRefund previous = new ReceiptRefund();
        previous.setAmount(new BigDecimal("95000.00"));

        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(accessContext.value(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.findById(10L)).thenReturn(Optional.of(receipt));
        when(idempotencyContext.amount(any())).thenReturn("10000");
        when(idempotencyContext.text(any())).thenReturn("客户退款");
        when(idempotencyContext.fingerprint(any())).thenReturn("refund-fingerprint-2");
        when(idempotencyContext.clientKeyOr(any(), any())).thenReturn("refund-key-2");
        when(refundRepository.findByCompany_ComIdAndIdempotencyKey(1L, "refund-key-2")).thenReturn(Optional.empty());
        when(refundRepository.findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(1L, 10L))
                .thenReturn(List.of(previous));

        assertThatThrownBy(() -> context.create(finance, 10L, new BigDecimal("10000.00"),
                null, LocalDate.of(2026, 10, 7), "超额退款", null))
                .hasMessageContaining("退款金额不能超过");
    }
}
