package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 应收计划筛选结果汇总，金额由服务端按全部匹配记录聚合计算。 */
@Note("应收计划汇总")
public class FinanceReceivableReport {

    @Note("匹配应收节点数")
    private Integer count = 0;

    @Unit("元")
    @Note("应收总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("累计已收金额")
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("待收金额 = Σ max(应收 - 已收, 0)")
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("逾期未收金额")
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
