package com.th4.edge.decoboon.quote.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 财务报价单行汇总，不包含分页参数。 */
@Note("财务报价汇总")
public class QuoteFinanceSummary {

    @Note("统计截止日期")
    private LocalDate asOf;

    @Note("报价总数")
    private Long quoteCount = 0L;

    @Unit("元")
    @Note("报价总额")
    private BigDecimal quoteAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("预计毛利（仅成本完整报价）")
    private BigDecimal profitAmount = BigDecimal.ZERO;

    @Note("成本未完整报价数")
    private Long costIncompleteCount = 0L;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public Long getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Long quoteCount) { this.quoteCount = quoteCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public Long getCostIncompleteCount() { return costIncompleteCount; }
    public void setCostIncompleteCount(Long costIncompleteCount) { this.costIncompleteCount = costIncompleteCount; }
}
