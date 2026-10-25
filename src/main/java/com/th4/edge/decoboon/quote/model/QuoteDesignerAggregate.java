package com.th4.edge.decoboon.quote.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/**
 * 财务报价按设计师聚合投影（DEC-036 正模式：聚合 SQL 直接投影，不加载实体）。
 */
@Note("财务报价按设计师聚合")
public class QuoteDesignerAggregate {

    @Note("设计师员工标识")
    private Long designerId;

    @Note("设计师姓名")
    private String designerName;

    @Note("报价数")
    private Long quoteCount = 0L;

    @Unit("元")
    @Note("报价总额（服务端报价头金额快照求和）")
    private BigDecimal quoteAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("预计毛利（仅成本完整报价的 金额-成本 求和）")
    private BigDecimal profitAmount = BigDecimal.ZERO;

    @Note("成本未完整报价数")
    private Long costIncompleteCount = 0L;

    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public Long getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Long quoteCount) { this.quoteCount = quoteCount; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public Long getCostIncompleteCount() { return costIncompleteCount; }
    public void setCostIncompleteCount(Long costIncompleteCount) { this.costIncompleteCount = costIncompleteCount; }
}
