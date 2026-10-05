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
    private BigDecimal monthNetCashFlowAmount = BigDecimal.ZERO;
    private Integer receiptCount = 0;
    private Integer paymentCount = 0;
    private BigDecimal receiptAmount = BigDecimal.ZERO;
    private BigDecimal paymentAmount = BigDecimal.ZERO;
    private BigDecimal monthReceiptAmount = BigDecimal.ZERO;
    private BigDecimal monthPaymentAmount = BigDecimal.ZERO;
    private BigDecimal urgentPayableAmount = BigDecimal.ZERO;
    private BigDecimal cashBalance = BigDecimal.ZERO;
    private BigDecimal openingBalanceAmount = BigDecimal.ZERO;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private BigDecimal actualCost = BigDecimal.ZERO;
    /** 手工记账中关联项目的其他收入，已计入项目收入口径。 */
    private BigDecimal manualIncomeAmount = BigDecimal.ZERO;
    /** 手工记账中项目零星成本，已计入项目实际成本口径。 */
    private BigDecimal manualCostAmount = BigDecimal.ZERO;
    private BigDecimal profitAmount;
    /** 汇总毛利率；成本不完整时为 null（不得用 0 代替）。 */
    private BigDecimal grossMargin;
    private Boolean costComplete = Boolean.TRUE;
    private Integer costIncompleteProjectCount = 0;
    private Integer costIncompleteItemCount = 0;
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
    public BigDecimal getMonthNetCashFlowAmount() { return monthNetCashFlowAmount; }
    public void setMonthNetCashFlowAmount(BigDecimal monthNetCashFlowAmount) { this.monthNetCashFlowAmount = monthNetCashFlowAmount; }
    public Integer getReceiptCount() { return receiptCount; }
    public void setReceiptCount(Integer receiptCount) { this.receiptCount = receiptCount; }
    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }
    public BigDecimal getReceiptAmount() { return receiptAmount; }
    public void setReceiptAmount(BigDecimal receiptAmount) { this.receiptAmount = receiptAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getMonthReceiptAmount() { return monthReceiptAmount; }
    public void setMonthReceiptAmount(BigDecimal monthReceiptAmount) { this.monthReceiptAmount = monthReceiptAmount; }
    public BigDecimal getMonthPaymentAmount() { return monthPaymentAmount; }
    public void setMonthPaymentAmount(BigDecimal monthPaymentAmount) { this.monthPaymentAmount = monthPaymentAmount; }
    public BigDecimal getUrgentPayableAmount() { return urgentPayableAmount; }
    public void setUrgentPayableAmount(BigDecimal urgentPayableAmount) { this.urgentPayableAmount = urgentPayableAmount; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public BigDecimal getOpeningBalanceAmount() { return openingBalanceAmount; }
    public void setOpeningBalanceAmount(BigDecimal openingBalanceAmount) { this.openingBalanceAmount = openingBalanceAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public BigDecimal getManualIncomeAmount() { return manualIncomeAmount; }
    public void setManualIncomeAmount(BigDecimal manualIncomeAmount) { this.manualIncomeAmount = manualIncomeAmount; }
    public BigDecimal getManualCostAmount() { return manualCostAmount; }
    public void setManualCostAmount(BigDecimal manualCostAmount) { this.manualCostAmount = manualCostAmount; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public Integer getCostIncompleteProjectCount() { return costIncompleteProjectCount; }
    public void setCostIncompleteProjectCount(Integer costIncompleteProjectCount) { this.costIncompleteProjectCount = costIncompleteProjectCount; }
    public Integer getCostIncompleteItemCount() { return costIncompleteItemCount; }
    public void setCostIncompleteItemCount(Integer costIncompleteItemCount) { this.costIncompleteItemCount = costIncompleteItemCount; }
    public BigDecimal getCollectionRate() { return collectionRate; }
    public void setCollectionRate(BigDecimal collectionRate) { this.collectionRate = collectionRate; }
    public List<Receivable> getUrgentReceivables() { return urgentReceivables; }
    public void setUrgentReceivables(List<Receivable> urgentReceivables) { this.urgentReceivables = urgentReceivables; }
    public List<Payable> getUrgentPayables() { return urgentPayables; }
    public void setUrgentPayables(List<Payable> urgentPayables) { this.urgentPayables = urgentPayables; }
    public List<Ledger> getRecentLedgers() { return recentLedgers; }
    public void setRecentLedgers(List<Ledger> recentLedgers) { this.recentLedgers = recentLedgers; }
}
