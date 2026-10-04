package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 资金台账筛选结果汇总，金额由服务端按全部匹配记录计算。 */
@Note("资金台账汇总")
public class FinanceLedgerReport {

    @Note("匹配流水笔数")
    private Integer count = 0;

    @Unit("元")
    @Note("匹配流水金额绝对值合计，用于已选流水合计")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("收入合计")
    private BigDecimal incomeAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("支出合计")
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("净现金流 = 收入合计 - 支出合计")
    private BigDecimal netCashFlowAmount = BigDecimal.ZERO;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getIncomeAmount() { return incomeAmount; }
    public void setIncomeAmount(BigDecimal incomeAmount) { this.incomeAmount = incomeAmount; }
    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }
    public BigDecimal getNetCashFlowAmount() { return netCashFlowAmount; }
    public void setNetCashFlowAmount(BigDecimal netCashFlowAmount) { this.netCashFlowAmount = netCashFlowAmount; }
}
