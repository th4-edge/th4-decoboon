package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.payroll.context.CommissionAdjustmentContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** 客户退款事实、资金台账和提成负向调整联动。 */
@Component
public class ReceiptRefundContext {

    private final ReceiptRefundRepository refundRepository;
    private final ReceiptRepository receiptRepository;
    private final FinanceAccessContext accessContext;
    private final FinanceIdempotencyContext idempotencyContext;
    private final CommissionAdjustmentContext commissionAdjustmentContext;

    public ReceiptRefundContext(ReceiptRefundRepository refundRepository,
                                ReceiptRepository receiptRepository,
                                FinanceAccessContext accessContext,
                                FinanceIdempotencyContext idempotencyContext,
                                CommissionAdjustmentContext commissionAdjustmentContext) {
        this.refundRepository = refundRepository;
        this.receiptRepository = receiptRepository;
        this.accessContext = accessContext;
        this.idempotencyContext = idempotencyContext;
        this.commissionAdjustmentContext = commissionAdjustmentContext;
    }

    @Transactional
    public ReceiptRefund create(Employee principal, Long receiptId, BigDecimal amount,
                                FundAccount account, LocalDate refundAt, String reason,
                                String idempotencyKey) {
        Employee current = accessContext.requireEditor(principal);
        if (receiptId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "原收款不能为空"));
        }
        BigDecimal paid = amount == null ? BigDecimal.ZERO : amount;
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "退款金额必须大于 0"));
        }
        FundAccount targetAccount = account == null ? FundAccount.CompanyBank : account;
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "原收款不存在或无权访问")));
        String fingerprintBase = String.join("|", "REFUND",
                String.valueOf(receipt.getReceiptId()), idempotencyContext.amount(paid),
                targetAccount.name(), refundAt == null ? "" : refundAt.toString(),
                idempotencyContext.text(reason));
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey, fingerprint);
        Optional<ReceiptRefund> existing = refundRepository
                .findByCompany_ComIdAndIdempotencyKey(current.getCompany().getComId(), key);
        if (existing.isPresent()) {
            ReceiptRefund found = existing.get();
            if (fingerprint.equals(found.getIdempotencyFingerprint())) {
                return found;
            }
            throw idempotencyContext.conflict();
        }

        BigDecimal alreadyRefunded = refundRepository
                .findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(
                        current.getCompany().getComId(), receiptId)
                .stream().map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundable = accessContext.value(receipt.getAmount()).subtract(alreadyRefunded);
        if (paid.compareTo(refundable) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "退款金额不能超过原收款可退金额"));
        }

        ReceiptRefund refund = new ReceiptRefund();
        accessContext.applyOwnership(refund, current, receipt.getProject());
        refund.setReceipt(receipt);
        refund.setCustomer(receipt.getCustomer());
        refund.setAmount(paid.setScale(2, RoundingMode.HALF_UP));
        refund.setAccount(targetAccount);
        refund.setRefundAt(refundAt == null ? LocalDate.now() : refundAt);
        refund.setReason(reason);
        refund.setIdempotencyKey(key);
        refund.setIdempotencyFingerprint(fingerprint);
        refund.setCreatedBy(current);
        ReceiptRefund saved;
        try {
            saved = refundRepository.saveAndFlush(refund);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyContext.isDuplicateKey(exception)) {
                throw idempotencyContext.conflict();
            }
            throw exception;
        }

        accessContext.recordLedger(current, receipt.getProject(), LedgerDirection.Expense,
                "客户退款", LedgerSource.Refund, saved.getRefundId(), paid, targetAccount,
                saved.getRefundAt(), receipt.getCustomer() == null ? null : receipt.getCustomer().getName(),
                "退款 · " + receipt.getProject().getName(), reason);
        commissionAdjustmentContext.applyRefund(current, saved);
        return saved;
    }

    private BigDecimal money(ReceiptRefund refund) {
        return refund == null || refund.getAmount() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : refund.getAmount().setScale(2, RoundingMode.HALF_UP);
    }
}
