package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 财务工作台企业级汇总视图。 */
@Note("财务工作台")
public class FinanceSummary {

    private Integer projectCount = 0;
    private Integer activeProjectCount = 0;
    private BigDecimal contractAmount = BigDecimal.ZERO;
    private BigDecimal receivableAmount = BigDecimal.ZERO;
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    private BigDecimal overdueAmount = BigDecimal.ZERO;
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
    private BigDecimal materialPayableAmount = BigDecimal.ZERO;
    private BigDecimal salaryPayableAmount = BigDecimal.ZERO;
    private BigDecimal projectPayableAmount = BigDecimal.ZERO;
    private BigDecimal otherPayableAmount = BigDecimal.ZERO;
    private BigDecimal monthIncome = BigDecimal.ZERO;
    private BigDecimal monthExpense = BigDecimal.ZERO;
    private BigDecimal cashBalance = BigDecimal.ZERO;
    private BigDecimal profitAmount = BigDecimal.ZERO;
    private BigDecimal collectionRate = BigDecimal.ZERO;
    private List<Receivable> urgentReceivables = List.of();
    private List<Payable> urgentPayables = List.of();
    private List<Ledger> recentLedgers = List.of();

    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(Integer activeProjectCount) { this.activeProjectCount = activeProjectCount; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount; }
    public BigDecimal getMaterialPayableAmount() { return materialPayableAmount; }
    public void setMaterialPayableAmount(BigDecimal materialPayableAmount) { this.materialPayableAmount = materialPayableAmount; }
    public BigDecimal getSalaryPayableAmount() { return salaryPayableAmount; }
    public void setSalaryPayableAmount(BigDecimal salaryPayableAmount) { this.salaryPayableAmount = salaryPayableAmount; }
    public BigDecimal getProjectPayableAmount() { return projectPayableAmount; }
    public void setProjectPayableAmount(BigDecimal projectPayableAmount) { this.projectPayableAmount = projectPayableAmount; }
    public BigDecimal getOtherPayableAmount() { return otherPayableAmount; }
    public void setOtherPayableAmount(BigDecimal otherPayableAmount) { this.otherPayableAmount = otherPayableAmount; }
    public BigDecimal getMonthIncome() { return monthIncome; }
    public void setMonthIncome(BigDecimal monthIncome) { this.monthIncome = monthIncome; }
    public BigDecimal getMonthExpense() { return monthExpense; }
    public void setMonthExpense(BigDecimal monthExpense) { this.monthExpense = monthExpense; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public BigDecimal getCollectionRate() { return collectionRate; }
    public void setCollectionRate(BigDecimal collectionRate) { this.collectionRate = collectionRate; }
    public List<Receivable> getUrgentReceivables() { return urgentReceivables; }
    public void setUrgentReceivables(List<Receivable> urgentReceivables) { this.urgentReceivables = urgentReceivables; }
    public List<Payable> getUrgentPayables() { return urgentPayables; }
    public void setUrgentPayables(List<Payable> urgentPayables) { this.urgentPayables = urgentPayables; }
    public List<Ledger> getRecentLedgers() { return recentLedgers; }
    public void setRecentLedgers(List<Ledger> recentLedgers) { this.recentLedgers = recentLedgers; }
}
