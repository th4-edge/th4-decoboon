package com.th4.edge.decoboon.finance.model;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 单项目财务全景，用于项目财务页和结算校验。 */
@Note("项目财务全景")
public class FinanceProjectReport {

    private Project project;
    private BigDecimal contractAmount = BigDecimal.ZERO;
    private BigDecimal contractRevenue = BigDecimal.ZERO;
    private BigDecimal receivableAmount = BigDecimal.ZERO;
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    private BigDecimal overdueAmount = BigDecimal.ZERO;
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
    private BigDecimal materialAmount = BigDecimal.ZERO;
    private BigDecimal materialCost = BigDecimal.ZERO;
    private BigDecimal salaryAmount = BigDecimal.ZERO;
    private BigDecimal laborCost = BigDecimal.ZERO;
    private BigDecimal projectPaymentAmount = BigDecimal.ZERO;
    private BigDecimal projectCost = BigDecimal.ZERO;
    private BigDecimal otherPayableAmount = BigDecimal.ZERO;
    private BigDecimal expenseAllocation = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal profitAmount = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private BigDecimal grossMargin = BigDecimal.ZERO;
    private BigDecimal collectionRate = BigDecimal.ZERO;
    private BigDecimal paymentRate = BigDecimal.ZERO;
    private Boolean receivableCleared = Boolean.FALSE;
    private Boolean payableCleared = Boolean.FALSE;
    private Settlement settlement;
    private List<Receivable> receivables = List.of();
    private List<Payable> payables = List.of();
    private List<Ledger> recentLedgers = List.of();

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getContractRevenue() { return contractRevenue; }
    public void setContractRevenue(BigDecimal contractRevenue) { this.contractRevenue = contractRevenue; }
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
    public BigDecimal getMaterialAmount() { return materialAmount; }
    public void setMaterialAmount(BigDecimal materialAmount) { this.materialAmount = materialAmount; }
    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }
    public BigDecimal getSalaryAmount() { return salaryAmount; }
    public void setSalaryAmount(BigDecimal salaryAmount) { this.salaryAmount = salaryAmount; }
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    public BigDecimal getProjectPaymentAmount() { return projectPaymentAmount; }
    public void setProjectPaymentAmount(BigDecimal projectPaymentAmount) { this.projectPaymentAmount = projectPaymentAmount; }
    public BigDecimal getProjectCost() { return projectCost; }
    public void setProjectCost(BigDecimal projectCost) { this.projectCost = projectCost; }
    public BigDecimal getOtherPayableAmount() { return otherPayableAmount; }
    public void setOtherPayableAmount(BigDecimal otherPayableAmount) { this.otherPayableAmount = otherPayableAmount; }
    public BigDecimal getExpenseAllocation() { return expenseAllocation; }
    public void setExpenseAllocation(BigDecimal expenseAllocation) { this.expenseAllocation = expenseAllocation; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public BigDecimal getCollectionRate() { return collectionRate; }
    public void setCollectionRate(BigDecimal collectionRate) { this.collectionRate = collectionRate; }
    public BigDecimal getPaymentRate() { return paymentRate; }
    public void setPaymentRate(BigDecimal paymentRate) { this.paymentRate = paymentRate; }
    public Boolean getReceivableCleared() { return receivableCleared; }
    public void setReceivableCleared(Boolean receivableCleared) { this.receivableCleared = receivableCleared; }
    public Boolean getPayableCleared() { return payableCleared; }
    public void setPayableCleared(Boolean payableCleared) { this.payableCleared = payableCleared; }
    public Settlement getSettlement() { return settlement; }
    public void setSettlement(Settlement settlement) { this.settlement = settlement; }
    public List<Receivable> getReceivables() { return receivables; }
    public void setReceivables(List<Receivable> receivables) { this.receivables = receivables; }
    public List<Payable> getPayables() { return payables; }
    public void setPayables(List<Payable> payables) { this.payables = payables; }
    public List<Ledger> getRecentLedgers() { return recentLedgers; }
    public void setRecentLedgers(List<Ledger> recentLedgers) { this.recentLedgers = recentLedgers; }
}
