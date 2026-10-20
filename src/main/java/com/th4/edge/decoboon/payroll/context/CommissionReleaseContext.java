package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionReleaseRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 客户收款后按累计有效回款比例自动释放提成。 */
@Component
public class CommissionReleaseContext {

    private final CommissionItemRepository commissionRepository;
    private final CommissionReleaseRepository releaseRepository;
    private final ReceiptRepository receiptRepository;
    private final PayrollCalculationContext calculationContext;

    public CommissionReleaseContext(CommissionItemRepository commissionRepository,
                                    CommissionReleaseRepository releaseRepository,
                                    ReceiptRepository receiptRepository,
                                    PayrollCalculationContext calculationContext) {
        this.commissionRepository = commissionRepository;
        this.releaseRepository = releaseRepository;
        this.receiptRepository = receiptRepository;
        this.calculationContext = calculationContext;
    }

    @Transactional
    public void releaseForReceipt(Employee current, Receipt receipt) {
        if (receipt == null || receipt.getProject() == null) return;
        Long comId = current.getCompany().getComId();
        Long projectId = receipt.getProject().getProjectId();
        List<Receipt> receipts = receiptRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(comId, projectId);
        BigDecimal cumulative = receipts.stream()
                .filter(item -> item.getStatus() == ReceiptStatus.Active)
                .filter(item -> item.getReceiptId() == null || receipt.getReceiptId() == null
                        || item.getReceiptId() <= receipt.getReceiptId())
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        List<CommissionItem> commissions = commissionRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(comId, projectId);
        for (CommissionItem commission : commissions) {
            if (commission.getStatus() == CommissionStatus.Closed
                    || commission.getStatus() == CommissionStatus.Adjusted) continue;
            if (releaseRepository.findByCompany_ComIdAndCommissionItem_CommissionIdAndReceipt_ReceiptId(
                    comId, commission.getCommissionId(), receipt.getReceiptId()).isPresent()) continue;
            BigDecimal amount = calculationContext.commissionReleaseAmount(commission, cumulative);
            if (amount.signum() <= 0) continue;
            CommissionRelease release = new CommissionRelease();
            release.setCompany(commission.getCompany());
            release.setStore(commission.getStore());
            release.setTeam(commission.getTeam());
            release.setEmployee(commission.getEmployee());
            release.setCommissionItem(commission);
            release.setReceipt(receipt);
            release.setAmount(amount);
            release.setReleasedAt(receipt.getReceivedAt());
            release.setRemark("按累计有效回款比例自动释放");
            release.setCreatedBy(current);
            releaseRepository.save(release);
            commission.setReleasedAmount(money(commission.getReleasedAmount()).add(amount).setScale(2, RoundingMode.HALF_UP));
            commission.setStatus(CommissionStatus.Released);
            commissionRepository.save(commission);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }
}
