package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/**
 * 门店对比矩阵的一行（`P0D-05` / `DESIGN-BIZ-20261013-003`）。
 *
 * <p>未归属门店与已停用门店**单独成行**且 `ranked=false`（不参与排名）；成本不完整时 `grossProfit/grossMargin` 为 `null`。</p>
 */
@Note("门店对比行")
public class StoreComparisonRow {

    @Note("门店 ID；未归属行为 null")
    private Long storeId;
    @Note("门店名；未归属行为「未归属门店」")
    private String storeName;
    @Note("门店状态（Active 启用 / Disabled 已停用）")
    private String storeStatus;
    @Note("是否未归属门店行")
    private boolean unassigned = false;
    @Note("是否参与排名（仅启用门店参与）")
    private boolean ranked = false;
    @Note("排名（1 起；不参与排名为空）")
    private Integer rank;

    @Note("签约额（本月，MET-004 口径）")
    private BigDecimal contractAmount = BigDecimal.ZERO;
    @Note("回款额（本月，MET-005 口径）")
    private BigDecimal receiptAmount = BigDecimal.ZERO;
    @Note("应收合计")
    private BigDecimal receivableAmount = BigDecimal.ZERO;
    @Note("待收（应收 - 已收）")
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    @Note("应付合计")
    private BigDecimal payableAmount = BigDecimal.ZERO;
    @Note("待付（应付 - 已付）")
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    @Note("项目毛利（成本不完整为 null）")
    private BigDecimal grossProfit;
    @Note("毛利率 %（成本不完整为 null）")
    private BigDecimal grossMargin;
    @Note("成本是否完整")
    private boolean costComplete = true;
    @Note("成本不完整提示")
    private String costWarning;

    @Note("在建项目数")
    private Integer activeProjectCount = 0;
    @Note("客户数")
    private Integer customerCount = 0;
    @Note("在职人数（人效分母）")
    private Integer headcount = 0;
    @Note("人效 = 签约额 ÷ 该店在职人数")
    private BigDecimal perCapitaAmount = BigDecimal.ZERO;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreStatus() { return storeStatus; }
    public void setStoreStatus(String storeStatus) { this.storeStatus = storeStatus; }
    public boolean isUnassigned() { return unassigned; }
    public void setUnassigned(boolean unassigned) { this.unassigned = unassigned; }
    public boolean isRanked() { return ranked; }
    public void setRanked(boolean ranked) { this.ranked = ranked; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getReceiptAmount() { return receiptAmount; }
    public void setReceiptAmount(BigDecimal receiptAmount) { this.receiptAmount = receiptAmount; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public boolean isCostComplete() { return costComplete; }
    public void setCostComplete(boolean costComplete) { this.costComplete = costComplete; }
    public String getCostWarning() { return costWarning; }
    public void setCostWarning(String costWarning) { this.costWarning = costWarning; }
    public Integer getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(Integer activeProjectCount) { this.activeProjectCount = activeProjectCount; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getHeadcount() { return headcount; }
    public void setHeadcount(Integer headcount) { this.headcount = headcount; }
    public BigDecimal getPerCapitaAmount() { return perCapitaAmount; }
    public void setPerCapitaAmount(BigDecimal perCapitaAmount) { this.perCapitaAmount = perCapitaAmount; }
}
