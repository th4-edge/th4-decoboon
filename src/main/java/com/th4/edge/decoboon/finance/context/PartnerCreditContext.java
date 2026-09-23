package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.model.PartnerCredit;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PartnerCreditRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商往来余额抵扣。
 *
 * <p>退货形成的往来余额在下一次形成应付时自动抵扣（按形成先后 FIFO），
 * 抵扣后余额归零或剩余继续留存；不修改历史付款流水。</p>
 */
@Note("供应商往来余额抵扣")
@Component
public class PartnerCreditContext {

    private final PartnerCreditRepository creditRepository;
    private final PayableRepository payableRepository;

    public PartnerCreditContext(PartnerCreditRepository creditRepository,
                                PayableRepository payableRepository) {
        this.creditRepository = creditRepository;
        this.payableRepository = payableRepository;
    }

    /** 用该供应商的往来余额抵扣应付；返回实际抵扣金额。 */
    @Transactional
    public BigDecimal applyTo(Payable payable) {
        if (payable == null || payable.getPayeePartner() == null
                || payable.getPayeePartner().getPartnerId() == null || payable.getCompany() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal reducible = value(payable.getAmount()).subtract(value(payable.getPaidAmount()));
        if (reducible.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<PartnerCredit> credits = creditRepository
                .findByCompany_ComIdAndPartner_PartnerIdOrderByPartnerCreditIdDesc(
                        payable.getCompany().getComId(), payable.getPayeePartner().getPartnerId());
        // 按形成先后（ID 升序）抵扣
        List<PartnerCredit> ordered = credits.stream()
                .sorted((left, right) -> Long.compare(
                        left.getPartnerCreditId() == null ? 0L : left.getPartnerCreditId(),
                        right.getPartnerCreditId() == null ? 0L : right.getPartnerCreditId()))
                .toList();
        BigDecimal remaining = reducible;
        BigDecimal used = BigDecimal.ZERO;
        for (PartnerCredit credit : ordered) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal balance = value(credit.getAmount());
            if (balance.signum() <= 0) {
                continue;
            }
            BigDecimal cut = balance.min(remaining);
            credit.setAmount(balance.subtract(cut));
            credit.setRemark(append(credit.getRemark(), "抵扣应付 ¥" + cut.toPlainString()
                    + (payable.getName() == null ? "" : "（" + payable.getName() + "）")));
            creditRepository.save(credit);
            used = used.add(cut);
            remaining = remaining.subtract(cut);
        }
        if (used.signum() > 0) {
            payable.setAmount(value(payable.getAmount()).subtract(used));
            payable.setRemark(append(payable.getRemark(), "供应商往来余额抵扣 ¥" + used.toPlainString()));
            payableRepository.save(payable);
        }
        return used;
    }

    private String append(String origin, String extra) {
        if (origin == null || origin.isBlank()) {
            return extra;
        }
        String merged = origin + "；" + extra;
        return merged.length() > 500 ? merged.substring(0, 500) : merged;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
