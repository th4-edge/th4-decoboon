package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 客户退款筛选结果汇总。 */
@Note("客户退款汇总")
public class FinanceRefundReport {

    @Note("匹配退款笔数")
    private Integer count = 0;

    @Unit("元")
    @Note("退款总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
