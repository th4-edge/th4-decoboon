package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.SettlementMode;
import com.th4.edge.decoboon.supply.model.Partner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 应付到期日计算。
 *
 * <p>按供应商配置的账期规则计算：</p>
 * <ul>
 *   <li>{@link SettlementMode#ReceiptDays}：收货日 + 账期天数（默认 30 天）；</li>
 *   <li>{@link SettlementMode#MonthlyCutoff}：以每月固定截账日（默认 25 日）为账期切点，
 *       收货日不超过截账日则计入当月账期，否则计入次月账期，再顺延账期天数。</li>
 * </ul>
 */
@Component
public class PayableDueDateContext {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_CUTOFF_DAY = 25;
    private static final int MAX_CUTOFF_DAY = 28;

    /** 计算到期日；收货日为空时返回 null（表示尚未到账期起算点）。 */
    public LocalDate dueDate(Partner partner, LocalDate receiptDate) {
        if (receiptDate == null) {
            return null;
        }
        SettlementMode mode = partner != null && partner.getSettlementMode() != null
                ? partner.getSettlementMode()
                : SettlementMode.ReceiptDays;
        int days = partner != null && partner.getSettlementDays() != null
                ? Math.max(0, partner.getSettlementDays())
                : DEFAULT_DAYS;
        if (mode == SettlementMode.MonthlyCutoff) {
            return monthlyCutoffDate(partner, receiptDate).plusDays(days);
        }
        return receiptDate.plusDays(days);
    }

    private LocalDate monthlyCutoffDate(Partner partner, LocalDate receiptDate) {
        int configured = partner != null && partner.getCutoffDay() != null
                ? partner.getCutoffDay()
                : DEFAULT_CUTOFF_DAY;
        int cutoff = Math.min(Math.max(configured, 1), MAX_CUTOFF_DAY);
        if (receiptDate.getDayOfMonth() <= cutoff) {
            return receiptDate.withDayOfMonth(cutoff);
        }
        return receiptDate.plusMonths(1).withDayOfMonth(cutoff);
    }
}
