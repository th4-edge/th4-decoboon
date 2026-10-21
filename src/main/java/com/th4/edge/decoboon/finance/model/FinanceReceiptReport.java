package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 收款筛选结果汇总，金额由服务端按全部匹配记录聚合计算。 */
@Note("收款记录汇总")
public class FinanceReceiptReport {

    @Note("匹配收款笔数")
    private Integer receiptCount = 0;

    @Unit("元")
    @Note("收款总额（含已冲正收款）")
    private BigDecimal grossReceiptAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已冲正金额")
    private BigDecimal reversalAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("有效收款金额 = 收款总额 - 已冲正金额")
    private BigDecimal receiptAmount = BigDecimal.ZERO;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getReceiptCount() { return receiptCount; }
    public void setReceiptCount(Integer receiptCount) { this.receiptCount = receiptCount; }
    public BigDecimal getGrossReceiptAmount() { return grossReceiptAmount; }
    public void setGrossReceiptAmount(BigDecimal grossReceiptAmount) { this.grossReceiptAmount = grossReceiptAmount; }
    public BigDecimal getReversalAmount() { return reversalAmount; }
    public void setReversalAmount(BigDecimal reversalAmount) { this.reversalAmount = reversalAmount; }
    public BigDecimal getReceiptAmount() { return receiptAmount; }
    public void setReceiptAmount(BigDecimal receiptAmount) { this.receiptAmount = receiptAmount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
