package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收/应付账龄报表。
 *
 * <p>金额一律取「未结清余额」（应收 = 金额 - 已收；应付 = 金额 - 已付）；
 * {@code dueDate} 为空的历史数据单独归入「待确认账龄」，不并入任何正常档（设计方案 §11.2）。</p>
 */
@Note("应收应付账龄报表")
public class FinanceAgingReport {

    @Note("统计截止日")
    private LocalDate asOf;

    @Unit("元")
    @Note("应收未结清合计")
    private BigDecimal receivableTotal = BigDecimal.ZERO;

    @Note("应收账龄分档")
    private List<AgingBucket> receivableBuckets = List.of();

    @Unit("元")
    @Note("应付未结清合计")
    private BigDecimal payableTotal = BigDecimal.ZERO;

    @Note("应付账龄分档")
    private List<AgingBucket> payableBuckets = List.of();

    @Note("应收逾期率（%）= 逾期应收 ÷ 应收未结清")
    private BigDecimal receivableOverdueRate = BigDecimal.ZERO;

    @Note("应付逾期率（%）= 逾期应付 ÷ 应付未结清")
    private BigDecimal payableOverdueRate = BigDecimal.ZERO;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public BigDecimal getReceivableTotal() { return receivableTotal; }
    public void setReceivableTotal(BigDecimal receivableTotal) { this.receivableTotal = receivableTotal; }
    public List<AgingBucket> getReceivableBuckets() { return receivableBuckets; }
    public void setReceivableBuckets(List<AgingBucket> receivableBuckets) { this.receivableBuckets = receivableBuckets; }
    public BigDecimal getPayableTotal() { return payableTotal; }
    public void setPayableTotal(BigDecimal payableTotal) { this.payableTotal = payableTotal; }
    public List<AgingBucket> getPayableBuckets() { return payableBuckets; }
    public void setPayableBuckets(List<AgingBucket> payableBuckets) { this.payableBuckets = payableBuckets; }
    public BigDecimal getReceivableOverdueRate() { return receivableOverdueRate; }
    public void setReceivableOverdueRate(BigDecimal receivableOverdueRate) { this.receivableOverdueRate = receivableOverdueRate; }
    public BigDecimal getPayableOverdueRate() { return payableOverdueRate; }
    public void setPayableOverdueRate(BigDecimal payableOverdueRate) { this.payableOverdueRate = payableOverdueRate; }
}
