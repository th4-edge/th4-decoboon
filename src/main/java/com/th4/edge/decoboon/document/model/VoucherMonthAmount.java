package com.th4.edge.decoboon.document.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 凭证按月份的服务端汇总。 */
@Note("凭证月度汇总")
public class VoucherMonthAmount {

    @Note("月份，格式 yyyy-MM")
    private String month;

    @Note("凭证张数")
    private Integer count = 0;

    @Unit("元")
    @Note("凭证金额合计")
    private BigDecimal amount = BigDecimal.ZERO;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
