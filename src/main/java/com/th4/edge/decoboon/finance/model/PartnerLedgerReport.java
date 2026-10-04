package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.util.List;

/** 供应商往来台账及服务端汇总。 */
@Note("供应商往来报表")
public class PartnerLedgerReport {

    @Note("供应商明细")
    private List<PartnerLedger> rows = List.of();

    @Note("往来供应商数量")
    private Integer partnerCount = 0;

    @Unit("元")
    @Note("应付总额")
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("已付金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("待付金额")
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("逾期金额")
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    public List<PartnerLedger> getRows() { return rows; }
    public void setRows(List<PartnerLedger> rows) { this.rows = rows; }
    public Integer getPartnerCount() { return partnerCount; }
    public void setPartnerCount(Integer partnerCount) { this.partnerCount = partnerCount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
}
