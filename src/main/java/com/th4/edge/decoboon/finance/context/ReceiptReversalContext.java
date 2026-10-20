package com.th4.edge.decoboon.finance.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.components.lock.DistributedLocked;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.repository.VoucherItemRepository;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.ReceiptReversalReasonType;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
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
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 收款冲正：保留原收款，生成反向台账并恢复应收与未发放提成。 */
@Component
public class ReceiptReversalContext {

    private final ReceiptRepository receiptRepository;
    private final ReceiptReversalRepository reversalRepository;
    private final ReceivableRepository receivableRepository;
    private final ReceiptRefundRepository refundRepository;
    private final LedgerRepository ledgerRepository;
    private final VoucherItemRepository voucherItemRepository;
    private final CommissionReleaseRepository commissionReleaseRepository;
    private final CommissionItemRepository commissionItemRepository;
    private final FinanceAccessContext accessContext;
    private final FinanceIdempotencyContext idempotencyContext;
    private final ObjectMapper objectMapper;

    public ReceiptReversalContext(ReceiptRepository receiptRepository,
                                  ReceiptReversalRepository reversalRepository,
                                  ReceivableRepository receivableRepository,
                                  ReceiptRefundRepository refundRepository,
                                  LedgerRepository ledgerRepository,
                                  VoucherItemRepository voucherItemRepository,
                                  CommissionReleaseRepository commissionReleaseRepository,
                                  CommissionItemRepository commissionItemRepository,
                                  FinanceAccessContext accessContext,
                                  FinanceIdempotencyContext idempotencyContext,
                                  ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.reversalRepository = reversalRepository;
        this.receivableRepository = receivableRepository;
        this.refundRepository = refundRepository;
        this.ledgerRepository = ledgerRepository;
        this.voucherItemRepository = voucherItemRepository;
        this.commissionReleaseRepository = commissionReleaseRepository;
        this.commissionItemRepository = commissionItemRepository;
        this.accessContext = accessContext;
        this.idempotencyContext = idempotencyContext;
        this.objectMapper = objectMapper;
    }

    public String reverseLockKey(Employee principal, Long receiptId) {
        Long comId = principal == null || principal.getCompany() == null ? null : principal.getCompany().getComId();
        return "receipt-reversal:" + comId + ":" + receiptId;
    }

    @DistributedLocked(key = "#root.reverseLockKey(#principal, #receiptId)")
    @Transactional
    public ReceiptReversal reverse(Employee principal, Long receiptId,
                                   ReceiptReversalReasonType reasonType, String reason,
                                   LocalDate reversalAt, String idempotencyKey) {
        Employee current = accessContext.requireHighRiskEditor(principal);
        if (reasonType == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择收款冲正原因"));
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写收款冲正说明"));
        }
        Receipt receipt = receiptRepository.findByCompany_ComIdAndReceiptId(
                        current.getCompany().getComId(), receiptId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "收款记录不存在或无权访问")));
        accessContext.requireReceiptScope(current, receipt);

        String fingerprintBase = String.join("|",
                "RECEIPT_REVERSAL",
                String.valueOf(receipt.getReceiptId()),
                idempotencyContext.amount(receipt.getAmount()),
                reasonType.name(),
                idempotencyContext.text(normalizedReason),
                reversalAt == null ? "" : reversalAt.toString());
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey,
                idempotencyContext.sha256(fingerprintBase));

        Optional<ReceiptReversal> existingByKey = reversalRepository
                .findByCompany_ComIdAndIdempotencyKey(current.getCompany().getComId(), key);
        if (existingByKey.isPresent()) {
            ReceiptReversal found = existingByKey.get();
            if (fingerprint.equals(found.getIdempotencyFingerprint())) {
                return found;
            }
            throw idempotencyContext.conflict();
        }
        if (reversalRepository.findByCompany_ComIdAndReceipt_ReceiptId(
                current.getCompany().getComId(), receiptId).isPresent()
                || receipt.getStatus() == ReceiptStatus.Reversed) {
            throw new DefinedException(new DefaultErrorCode("409", "该收款已冲正，不能重复操作"));
        }
        if (receipt.getStatus() != ReceiptStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "只有有效收款可以冲正"));
        }
        if (!refundRepository.findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(
                current.getCompany().getComId(), receiptId).isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该收款已存在客户退款，不能按收款冲正处理"));
        }
        boolean hasVoucher = voucherItemRepository
                .findByCompany_ComIdAndSourceIdIn(current.getCompany().getComId(), List.of(receiptId))
                .stream().anyMatch(item -> item.getSourceType() == DocumentType.Receipt);
        if (hasVoucher) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该收款已生成凭证，请先作废凭证后再冲正"));
        }
        Ledger originalLedger = ledgerRepository
                .findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(
                        current.getCompany().getComId(), LedgerSource.Receipt, receiptId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("500", "原收款台账缺失，暂不能冲正")));

        List<CommissionRelease> releases = commissionReleaseRepository
                .findByCompany_ComIdAndReceipt_ReceiptId(current.getCompany().getComId(), receiptId)
                .stream().filter(item -> !Boolean.TRUE.equals(item.getReversed())).toList();
        for (CommissionRelease release : releases) {
            CommissionItem commission = release.getCommissionItem();
            if (commission == null
                    || accessContext.value(commission.getPaidAmount()).signum() > 0
                    || commission.getStatus() == CommissionStatus.InPayment
                    || commission.getStatus() == CommissionStatus.Paid
                    || commission.getStatus() == CommissionStatus.Closed) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "该收款关联的提成已发放或已进入发放，请先处理提成后再冲正"));
            }
        }

        LocalDate effectiveReversalAt = reversalAt == null ? LocalDate.now() : reversalAt;
        String before = snapshot(receipt, originalLedger);
        ReceiptReversal reversal = new ReceiptReversal();
        reversal.setCompany(receipt.getCompany());
        reversal.setStore(receipt.getStore());
        reversal.setTeam(receipt.getTeam());
        reversal.setOwner(receipt.getOwner());
        reversal.setReceipt(receipt);
        reversal.setProject(receipt.getProject());
        reversal.setCustomer(receipt.getCustomer());
        reversal.setReceivable(receipt.getReceivable());
        reversal.setAmount(money(receipt.getAmount()));
        reversal.setReversalAt(effectiveReversalAt);
        reversal.setReasonType(reasonType);
        reversal.setReason(normalizedReason);
        reversal.setIdempotencyKey(key);
        reversal.setIdempotencyFingerprint(fingerprint);
        reversal.setOperator(current);
        reversal.setBeforeSnapshot(before);
        reversal.setAfterSnapshot("{}");
        try {
            reversal = reversalRepository.saveAndFlush(reversal);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyContext.isDuplicateKey(exception)) {
                throw idempotencyContext.conflict();
            }
            throw exception;
        }

        Ledger reversalLedger = accessContext.recordLedger(current, receipt.getProject(),
                LedgerDirection.Expense, "收款冲正", LedgerSource.ReceiptReversal,
                reversal.getReversalId(), receipt.getAmount(), receipt.getAccount(),
                effectiveReversalAt, receipt.getPayerName(),
                "收款冲正 · " + receipt.getReceivable().getName(), normalizedReason);
        String groupId = "receipt-reversal-" + reversal.getReversalId();
        reversalLedger.setOriginalLedger(originalLedger);
        reversalLedger.setCorrectionGroupId(groupId);
        reversalLedger = ledgerRepository.save(reversalLedger);
        originalLedger.setCorrectionGroupId(groupId);
        ledgerRepository.save(originalLedger);
        reversal.setReversalLedger(reversalLedger);

        Receivable receivable = receipt.getReceivable();
        BigDecimal nextReceived = accessContext.value(receivable.getReceivedAmount())
                .subtract(accessContext.value(receipt.getAmount()))
                .max(BigDecimal.ZERO);
        receivable.setReceivedAmount(money(nextReceived));
        accessContext.refreshReceivableStatus(receivable);
        receivableRepository.save(receivable);

        receipt.setStatus(ReceiptStatus.Reversed);
        receipt.setReversedAt(LocalDateTime.now());
        receipt.setReversedBy(current);
        receipt.setReversalReason(normalizedReason);
        receiptRepository.save(receipt);

        for (CommissionRelease release : releases) {
            CommissionItem commission = release.getCommissionItem();
            BigDecimal nextReleased = accessContext.value(commission.getReleasedAmount())
                    .subtract(accessContext.value(release.getAmount()))
                    .max(BigDecimal.ZERO);
            commission.setReleasedAmount(money(nextReleased));
            if (nextReleased.signum() <= 0 && commission.getStatus() == CommissionStatus.Released) {
                commission.setStatus(CommissionStatus.Accrued);
            }
            commissionItemRepository.save(commission);

            release.setReversed(Boolean.TRUE);
            release.setReversedAt(LocalDateTime.now());
            release.setReversedBy(current);
            release.setReverseReason(normalizedReason);
            commissionReleaseRepository.save(release);
        }

        reversal.setAfterSnapshot(snapshot(receipt, originalLedger));
        return reversalRepository.save(reversal);
    }

    private String snapshot(Receipt receipt, Ledger originalLedger) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("receiptId", receipt.getReceiptId());
        value.put("receiptStatus", receipt.getStatus());
        value.put("amount", receipt.getAmount());
        value.put("receivedAt", receipt.getReceivedAt());
        value.put("receivableId", receipt.getReceivable() == null ? null : receipt.getReceivable().getReceivableId());
        value.put("receivableAmount", receipt.getReceivable() == null ? null : receipt.getReceivable().getAmount());
        value.put("receivableReceivedAmount", receipt.getReceivable() == null ? null : receipt.getReceivable().getReceivedAmount());
        value.put("receivableStatus", receipt.getReceivable() == null ? null : receipt.getReceivable().getStatus());
        value.put("originalLedgerId", originalLedger == null ? null : originalLedger.getLedgerId());
        value.put("reversedAt", receipt.getReversedAt());
        value.put("reversedBy", receipt.getReversedBy() == null ? null : receipt.getReversedBy().getEmpId());
        value.put("reversalReason", receipt.getReversalReason());
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DefinedException(new DefaultErrorCode("500", "冲正快照生成失败"));
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
