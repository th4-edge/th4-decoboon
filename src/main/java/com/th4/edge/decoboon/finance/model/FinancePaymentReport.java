package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 付款筛选结果汇总，金额由服务端按全部匹配记录聚合计算。 */
@Note("付款记录汇总")
public class FinancePaymentReport {

    @Note("匹配付款笔数")
    private Integer paymentCount = 0;

    @Unit("元")
    @Note("付款总额")
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
