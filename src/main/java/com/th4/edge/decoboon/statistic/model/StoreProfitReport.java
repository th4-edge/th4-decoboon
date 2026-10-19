package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 门店经营利润（参考）报告，口径见 MET-011（草案）。 */
@Note("门店经营利润（参考）")
public class StoreProfitReport {

    private Long storeId;
    private String storeName;
    private String periodLabel = "累计";
    @Unit("元") private BigDecimal projectGrossProfit;
    @Unit("元") private BigDecimal otherIncome = BigDecimal.ZERO;
    @Unit("元") private BigDecimal directCompanyExpense = BigDecimal.ZERO;
    @Unit("元") private BigDecimal storeProfit;
    private Boolean costComplete;
    private String profitWarning;
    private String unallocatedNote = "未分摊总部/公共费用";

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    public BigDecimal getProjectGrossProfit() { return projectGrossProfit; }
    public void setProjectGrossProfit(BigDecimal projectGrossProfit) { this.projectGrossProfit = projectGrossProfit; }
    public BigDecimal getOtherIncome() { return otherIncome; }
    public void setOtherIncome(BigDecimal otherIncome) { this.otherIncome = otherIncome; }
    public BigDecimal getDirectCompanyExpense() { return directCompanyExpense; }
    public void setDirectCompanyExpense(BigDecimal directCompanyExpense) { this.directCompanyExpense = directCompanyExpense; }
    public BigDecimal getStoreProfit() { return storeProfit; }
    public void setStoreProfit(BigDecimal storeProfit) { this.storeProfit = storeProfit; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public String getProfitWarning() { return profitWarning; }
    public void setProfitWarning(String profitWarning) { this.profitWarning = profitWarning; }
    public String getUnallocatedNote() { return unallocatedNote; }
    public void setUnallocatedNote(String unallocatedNote) { this.unallocatedNote = unallocatedNote; }
}
