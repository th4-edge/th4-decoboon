package com.th4.edge.decoboon.payroll.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 员工提成筛选结果汇总。 */
@Note("员工提成汇总")
public class FinanceCommissionReport {

    @Note("匹配提成记录数")
    private Integer count = 0;

    @Unit("元")
    @Note("提成金额")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已计提金额")
    private BigDecimal accruedAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已释放金额")
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已发放金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getAccruedAmount() { return accruedAmount; }
    public void setAccruedAmount(BigDecimal accruedAmount) { this.accruedAmount = accruedAmount; }
    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public void setReleasedAmount(BigDecimal releasedAmount) { this.releasedAmount = releasedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
