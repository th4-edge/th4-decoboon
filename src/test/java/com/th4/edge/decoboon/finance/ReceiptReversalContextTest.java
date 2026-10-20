package com.th4.edge.decoboon.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.document.repository.VoucherItemRepository;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.ReceiptReversalReasonType;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.context.ReceiptReversalContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptReversal;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptReversalRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionReleaseRepository;
import com.th4.edge.framework.exception.DefinedException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 收款冲正主链路：保留原收款，生成反向台账，恢复应收并幂等防重。 */
class ReceiptReversalContextTest {

    private final ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
    private final ReceiptReversalRepository reversalRepository = mock(ReceiptReversalRepository.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final ReceiptRefundRepository refundRepository = mock(ReceiptRefundRepository.class);
    private final LedgerRepository ledgerRepository = mock(LedgerRepository.class);
    private final VoucherItemRepository voucherRepository = mock(VoucherItemRepository.class);
    private final CommissionReleaseRepository releaseRepository = mock(CommissionReleaseRepository.class);
    private final CommissionItemRepository commissionRepository = mock(CommissionItemRepository.class);
    private final FinanceAccessContext accessContext = mock(FinanceAccessContext.class);
    private final ReceiptReversalContext context = new ReceiptReversalContext(
            receiptRepository, reversalRepository, receivableRepository, refundRepository, ledgerRepository,
            voucherRepository, releaseRepository, commissionRepository, accessContext,
            new FinanceIdempotencyContext(), new ObjectMapper().findAndRegisterModules());

    @Test
    void shouldReverseReceiptWithoutDeletingOriginalFacts() {
        Fixture fixture = fixture("1000.00", "1000.00");
        when(accessContext.recordLedger(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Ledger());

        ReceiptReversal reversal = context.reverse(fixture.boss, fixture.receipt.getReceiptId(),
                ReceiptReversalReasonType.NotReceived, "客户实际未付款", LocalDate.of(2026, 10, 20), "reverse-1");

        assertThat(reversal.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(fixture.receipt.getStatus()).isEqualTo(ReceiptStatus.Reversed);
        assertThat(fixture.receipt.getReversedAt()).isNotNull();
        assertThat(fixture.receivable.getReceivedAmount()).isEqualByComparingTo("0.00");
        assertThat(reversal.getBeforeSnapshot()).contains("\"receiptStatus\":\"Active\"");
        assertThat(reversal.getAfterSnapshot()).contains("\"receiptStatus\":\"Reversed\"");
        verify(receiptRepository).save(fixture.receipt);
        verify(receivableRepository).save(fixture.receivable);
    }

    @Test
    void shouldRejectReceiptWithRefund() {
        Fixture fixture = fixture("1000.00", "1000.00");
        when(refundRepository.findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(
                1L, fixture.receipt.getReceiptId())).thenReturn(List.of(mock(com.th4.edge.decoboon.finance.model.ReceiptRefund.class)));

        assertThatThrownBy(() -> context.reverse(fixture.boss, fixture.receipt.getReceiptId(),
                ReceiptReversalReasonType.NotReceived, "客户实际未付款", LocalDate.now(), "reverse-refund"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("客户退款");
    }

    @Test
    void shouldRejectPaidCommissionRelease() {
        Fixture fixture = fixture("1000.00", "1000.00");
        CommissionItem commission = new CommissionItem();
        commission.setPaidAmount(new BigDecimal("100.00"));
        commission.setStatus(CommissionStatus.Paid);
        CommissionRelease release = new CommissionRelease();
        release.setCommissionItem(commission);
        release.setAmount(new BigDecimal("100.00"));
        when(releaseRepository.findByCompany_ComIdAndReceipt_ReceiptId(1L, fixture.receipt.getReceiptId()))
                .thenReturn(List.of(release));

        assertThatThrownBy(() -> context.reverse(fixture.boss, fixture.receipt.getReceiptId(),
                ReceiptReversalReasonType.NotReceived, "客户实际未付款", LocalDate.now(), "reverse-paid"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("提成已发放");
    }

    @Test
    void shouldRejectSecondReversal() {
        Fixture fixture = fixture("1000.00", "1000.00");
        fixture.receipt.setStatus(ReceiptStatus.Reversed);
        when(reversalRepository.findByCompany_ComIdAndReceipt_ReceiptId(1L, fixture.receipt.getReceiptId()))
                .thenReturn(Optional.of(new ReceiptReversal()));

        assertThatThrownBy(() -> context.reverse(fixture.boss, fixture.receipt.getReceiptId(),
                ReceiptReversalReasonType.NotReceived, "客户实际未付款", LocalDate.now(), "reverse-twice"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已冲正");
    }

    private Fixture fixture(String amount, String received) {
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(10L);
        boss.setCompany(company);
        Receivable receivable = new Receivable();
        receivable.setReceivableId(20L);
        receivable.setAmount(new BigDecimal(amount));
        receivable.setReceivedAmount(new BigDecimal(received));
        Receipt receipt = new Receipt();
        receipt.setReceiptId(30L);
        receipt.setCompany(company);
        receipt.setAmount(new BigDecimal(amount));
        receipt.setAccount(FundAccount.CompanyBank);
        receipt.setReceivedAt(LocalDate.of(2026, 10, 20));
        receipt.setReceivable(receivable);
        receipt.setStatus(ReceiptStatus.Active);
        Ledger original = new Ledger();
        original.setLedgerId(40L);
        when(accessContext.requireHighRiskEditor(boss)).thenReturn(boss);
        when(receiptRepository.findByCompany_ComIdAndReceiptId(1L, 30L)).thenReturn(Optional.of(receipt));
        when(ledgerRepository.findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(any(), any(), any()))
                .thenReturn(Optional.of(original));
        when(refundRepository.findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(1L, 30L))
                .thenReturn(List.of());
        when(voucherRepository.findByCompany_ComIdAndSourceIdIn(any(), any())).thenReturn(List.of());
        when(releaseRepository.findByCompany_ComIdAndReceipt_ReceiptId(1L, 30L)).thenReturn(List.of());
        when(accessContext.value(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? BigDecimal.ZERO : invocation.getArgument(0));
        when(reversalRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReceiptReversal reversal = invocation.getArgument(0);
            reversal.setReversalId(50L);
            return reversal;
        });
        when(reversalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reversalRepository.findByCompany_ComIdAndReceipt_ReceiptId(1L, 30L)).thenReturn(Optional.empty());
        when(reversalRepository.findByCompany_ComIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        return new Fixture(boss, receipt, receivable);
    }

    private record Fixture(Employee boss, Receipt receipt, Receivable receivable) {
    }
}
