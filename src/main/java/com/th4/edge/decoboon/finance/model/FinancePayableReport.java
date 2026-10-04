package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 应付筛选结果汇总，金额由服务端按全部匹配记录计算。 */
@Note("应付账款汇总")
public class FinancePayableReport {

    @Note("匹配应付笔数")
    private Integer count = 0;

    @Unit("元")
    @Note("应付总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已付金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("待付金额")
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("风险/加急金额：逾期或加急应付的待付金额")
    private BigDecimal urgentAmount = BigDecimal.ZERO;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getUrgentAmount() { return urgentAmount; }
    public void setUrgentAmount(BigDecimal urgentAmount) { this.urgentAmount = urgentAmount; }
}
