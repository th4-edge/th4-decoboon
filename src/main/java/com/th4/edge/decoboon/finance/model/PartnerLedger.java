package com.th4.edge.decoboon.finance.model;

import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 供应商应付台账（按供应商聚合的欠款与账期概览）。 */
@Note("供应商应付台账")
public class PartnerLedger {

    @Note("供应商")
    private Partner partner;

    @Note("应付总额")
    @Unit("元")
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Note("已付金额")
    @Unit("元")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Note("待付金额")
    @Unit("元")
    private BigDecimal outstanding = BigDecimal.ZERO;

    @Note("其中逾期金额")
    @Unit("元")
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    @Note("未结清笔数")
    private Integer pendingCount = 0;

    @Note("最近到期日")
    private LocalDate nearestDueDate;

    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getOutstanding() { return outstanding; }
    public void setOutstanding(BigDecimal outstanding) { this.outstanding = outstanding; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public LocalDate getNearestDueDate() { return nearestDueDate; }
    public void setNearestDueDate(LocalDate nearestDueDate) { this.nearestDueDate = nearestDueDate; }
}
