package com.th4.edge.decoboon.document.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.util.List;

/** 凭证筛选结果汇总，金额由服务端按全部匹配记录计算。 */
@Note("凭证汇总")
public class VoucherReport {

    @Note("匹配凭证张数")
    private Integer count = 0;

    @Unit("元")
    @Note("凭证金额合计")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("收款凭证金额")
    private BigDecimal receiptAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("付款凭证金额")
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Note("月份汇总")
    private List<VoucherMonthAmount> monthlyAmounts = List.of();

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getReceiptAmount() { return receiptAmount; }
    public void setReceiptAmount(BigDecimal receiptAmount) { this.receiptAmount = receiptAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public List<VoucherMonthAmount> getMonthlyAmounts() { return monthlyAmounts; }
    public void setMonthlyAmounts(List<VoucherMonthAmount> monthlyAmounts) { this.monthlyAmounts = monthlyAmounts; }
}
