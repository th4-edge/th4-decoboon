package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentType;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.CommissionAdjustmentRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** 已发提成的退款、离职、跨期负向调整，始终留待下期工资抵扣。 */
@Component
public class CommissionAdjustmentContext {

    private final CommissionAdjustmentRepository adjustmentRepository;
    private final CommissionItemRepository commissionRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptRefundRepository receiptRefundRepository;
    private final PayrollCalculationContext calculationContext;
    private final PayrollAccessContext accessContext;
    private final FinanceIdempotencyContext idempotencyContext;

    public CommissionAdjustmentContext(CommissionAdjustmentRepository adjustmentRepository,
                                       CommissionItemRepository commissionRepository,
                                       ReceiptRepository receiptRepository,
                                       ReceiptRefundRepository receiptRefundRepository,
                                       PayrollCalculationContext calculationContext,
                                       PayrollAccessContext accessContext,
                                       FinanceIdempotencyContext idempotencyContext) {
        this.adjustmentRepository = adjustmentRepository;
        this.commissionRepository = commissionRepository;
        this.receiptRepository = receiptRepository;
        this.receiptRefundRepository = receiptRefundRepository;
        this.calculationContext = calculationContext;
        this.accessContext = accessContext;
        this.idempotencyContext = idempotencyContext;
    }

    @Transactional
    public CommissionAdjustment create(Employee principal,
                                       Long originalCommissionId,
                                       CommissionAdjustmentType type,
                                       BigDecimal refundAmount,
                                       BigDecimal deductionAmount,
                                       String idempotencyKey,
                                       LocalDate occurredAt,
                                       String remark) {
        Employee current = accessContext.requireEditor(principal);
        if (type == null) {
            throw new DefinedException(new DefaultErrorCode("400", "负向调整类型不能为空"));
        }
        String fingerprintBase = String.join("|",
                "COMMISSION_ADJUSTMENT", String.valueOf(originalCommissionId), type.name(),
                idempotencyContext.amount(refundAmount), idempotencyContext.amount(deductionAmount),
                occurredAt == null ? "" : occurredAt.toString(), idempotencyContext.text(remark));
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey, fingerprint);
        var existingByKey = adjustmentRepository
                .findByCompany_ComIdAndIdempotencyKey(current.getCompany().getComId(), key);
        if (existingByKey.isPresent()) {
            CommissionAdjustment found = existingByKey.get();
            if (fingerprint.equals(found.getIdempotencyFingerprint())) {
                return found;
            }
            throw idempotencyContext.conflict();
        }
        CommissionItem original = commissionRepository.findById(originalCommissionId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "原提成不存在或无权访问")));
        BigDecimal paid = money(original.getPaidAmount());
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "原提成尚未实际发放，不能形成负向调整"));
        }
        List<CommissionAdjustment> existing = adjustmentRepository
                .findByCompany_ComIdAndOriginalCommission_CommissionIdOrderByAdjustmentIdAsc(
                        current.getCompany().getComId(), original.getCommissionId());
        BigDecimal reserved = existing.stream()
                .filter(item -> item.getStatus() != CommissionAdjustmentStatus.Cancelled)
                .map(item -> money(item.getDeductionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingDeductible = paid.subtract(reserved).setScale(2, RoundingMode.HALF_UP);
        if (remainingDeductible.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "原提成已无可调整金额"));
        }

        BigDecimal amount;
        BigDecimal effectiveRefund = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (type == CommissionAdjustmentType.Refund) {
            effectiveRefund = money(refundAmount);
            if (effectiveRefund.signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "退款金额必须大于 0"));
            }
            if (original.getProject() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "退款调整必须关联项目提成"));
            }
            BigDecimal receipts = receiptRepository
                    .findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(
                            current.getCompany().getComId(), original.getProject().getProjectId())
                    .stream()
                    .map(this::receiptAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal refunds = existing.stream()
                    .filter(item -> item.getStatus() != CommissionAdjustmentStatus.Cancelled)
                    .filter(item -> item.getType() == CommissionAdjustmentType.Refund)
                    .map(item -> money(item.getRefundAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(effectiveRefund);
            BigDecimal effectiveReceipt = receipts.subtract(refunds).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal allowedRelease = calculationContext.commissionCumulativeRelease(original, effectiveReceipt);
            amount = paid.subtract(allowedRelease).min(remainingDeductible).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "退款后未形成已发提成超额，无需负向调整"));
            }
        } else {
            amount = money(deductionAmount);
            if (amount.signum() <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "抵扣金额必须大于 0"));
            }
            if (amount.compareTo(remainingDeductible) > 0) {
                throw new DefinedException(new DefaultErrorCode("400", "抵扣金额不能超过原提成未调整的已发金额"));
            }
        }

        CommissionAdjustment adjustment = new CommissionAdjustment();
        accessContext.applyOwnership(adjustment, current, original);
        adjustment.setType(type);
        adjustment.setStatus(CommissionAdjustmentStatus.Pending);
        adjustment.setRefundAmount(effectiveRefund);
        adjustment.setDeductionAmount(amount);
        adjustment.setAppliedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        adjustment.setOccurredAt(occurredAt == null ? LocalDate.now() : occurredAt);
        adjustment.setRemark(remark);
        adjustment.setIdempotencyKey(key);
        adjustment.setIdempotencyFingerprint(fingerprint);
        try {
            return adjustmentRepository.saveAndFlush(adjustment);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyContext.isDuplicateKey(exception)) {
                throw idempotencyContext.conflict();
            }
            throw exception;
        }
    }

    /** 客户退款落账后，对所有已发提成自动形成本退款对应的负向调整。 */
    @Transactional
    public void applyRefund(Employee principal, ReceiptRefund refund) {
        Employee current = accessContext.requireEditor(principal);
        if (refund == null || refund.getProject() == null || refund.getRefundId() == null) return;
        Long comId = current.getCompany().getComId();
        Long projectId = refund.getProject().getProjectId();
        List<CommissionItem> commissions = commissionRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(comId, projectId);
        if (commissions.isEmpty()) return;
        BigDecimal receipts = receiptRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(comId, projectId)
                .stream().map(this::receiptAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = receiptRefundRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByRefundAtDescRefundIdDesc(comId, projectId)
                .stream().map(this::refundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal effectiveReceipt = receipts.subtract(refunds).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        for (CommissionItem original : commissions) {
            if (money(original.getPaidAmount()).signum() <= 0) continue;
            if (adjustmentRepository
                    .findByCompany_ComIdAndOriginalCommission_CommissionIdAndRefund_RefundId(
                            comId, original.getCommissionId(), refund.getRefundId()).isPresent()) {
                continue;
            }
            List<CommissionAdjustment> existing = adjustmentRepository
                    .findByCompany_ComIdAndOriginalCommission_CommissionIdOrderByAdjustmentIdAsc(
                            comId, original.getCommissionId());
            BigDecimal reserved = existing.stream()
                    .filter(item -> item.getStatus() != CommissionAdjustmentStatus.Cancelled)
                    .map(item -> money(item.getDeductionAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = money(original.getPaidAmount()).subtract(reserved)
                    .setScale(2, RoundingMode.HALF_UP);
            if (remaining.signum() <= 0) continue;
            BigDecimal allowedRelease = calculationContext.commissionCumulativeRelease(original, effectiveReceipt);
            BigDecimal amount = money(original.getPaidAmount()).subtract(allowedRelease)
                    .min(remaining).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) continue;

            CommissionAdjustment adjustment = new CommissionAdjustment();
            accessContext.applyOwnership(adjustment, current, original);
            adjustment.setRefund(refund);
            adjustment.setType(CommissionAdjustmentType.Refund);
            adjustment.setStatus(CommissionAdjustmentStatus.Pending);
            adjustment.setRefundAmount(money(refund.getAmount()));
            adjustment.setDeductionAmount(amount);
            adjustment.setAppliedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            adjustment.setOccurredAt(refund.getRefundAt() == null ? LocalDate.now() : refund.getRefundAt());
            adjustment.setRemark("客户退款自动负向调整");
            adjustment.setIdempotencyKey("refund-" + refund.getRefundId() + "-commission-" + original.getCommissionId());
            adjustment.setIdempotencyFingerprint(idempotencyContext.fingerprint(String.join("|",
                    "REFUND_ADJUSTMENT", String.valueOf(refund.getRefundId()),
                    String.valueOf(original.getCommissionId()), idempotencyContext.amount(amount))));
            adjustmentRepository.saveAndFlush(adjustment);
        }
    }

    /** 按下期工资可承受空间分配抵扣，未分配完的调整保持待下期状态。 */
    public BigDecimal apply(List<CommissionAdjustment> adjustments, BigDecimal capacity) {
        BigDecimal remainingCapacity = money(capacity).max(BigDecimal.ZERO);
        BigDecimal appliedTotal = BigDecimal.ZERO;
        for (CommissionAdjustment adjustment : adjustments) {
            if (adjustment.getStatus() != CommissionAdjustmentStatus.Pending) continue;
            BigDecimal target = money(adjustment.getDeductionAmount());
            BigDecimal already = money(adjustment.getAppliedAmount());
            BigDecimal remaining = target.subtract(already).max(BigDecimal.ZERO);
            BigDecimal applied = remaining.min(remainingCapacity).setScale(2, RoundingMode.HALF_UP);
            if (applied.signum() <= 0) continue;
            BigDecimal totalApplied = already.add(applied).setScale(2, RoundingMode.HALF_UP);
            adjustment.setAppliedAmount(totalApplied);
            adjustment.setStatus(totalApplied.compareTo(target) >= 0
                    ? CommissionAdjustmentStatus.Included
                    : CommissionAdjustmentStatus.Pending);
            remainingCapacity = remainingCapacity.subtract(applied);
            appliedTotal = appliedTotal.add(applied);
        }
        return appliedTotal.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void resetForDraftItem(Employee principal, Long itemId) {
        Employee current = accessContext.requireEditor(principal);
        List<CommissionAdjustment> linked = adjustmentRepository
                .findByCompany_ComIdAndPayrollItem_ItemIdAndStatus(
                        current.getCompany().getComId(), itemId, CommissionAdjustmentStatus.Included);
        List<CommissionAdjustment> partial = adjustmentRepository
                .findByCompany_ComIdAndPayrollItem_ItemIdAndStatus(
                        current.getCompany().getComId(), itemId, CommissionAdjustmentStatus.Pending);
        java.util.stream.Stream.concat(linked.stream(), partial.stream())
                .filter(item -> item.getStatus() != CommissionAdjustmentStatus.Cancelled)
                .forEach(item -> {
                    item.setAppliedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    item.setStatus(CommissionAdjustmentStatus.Pending);
                    item.setPayrollItem(null);
                    adjustmentRepository.save(item);
                });
    }

    @Transactional
    public void linkToPayrollItem(Employee principal, List<CommissionAdjustment> adjustments, PayrollItem item) {
        Employee current = accessContext.requireEditor(principal);
        for (CommissionAdjustment adjustment : adjustments) {
            if (adjustment.getCompany() == null
                    || !current.getCompany().getComId().equals(adjustment.getCompany().getComId())) continue;
            adjustment.setPayrollItem(item);
            adjustmentRepository.save(adjustment);
        }
    }

    private BigDecimal receiptAmount(Receipt receipt) {
        return receipt == null || receipt.getAmount() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : receipt.getAmount();
    }

    private BigDecimal refundAmount(ReceiptRefund refund) {
        return refund == null || refund.getAmount() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : money(refund.getAmount());
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
