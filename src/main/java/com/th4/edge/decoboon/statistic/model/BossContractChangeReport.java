package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 企业负责人增减项工作台报告。 */
@Note("企业负责人增减项工作台报告")
public class BossContractChangeReport {

    private LocalDateTime asOf;

    private Integer itemCount;
    private Integer pendingCount;
    private BigDecimal pendingAmount;
    private Integer approvedCount;
    private BigDecimal approvedAmount;
    private Integer confirmedCount;
    private BigDecimal confirmedNetAmount;
    private Integer thisMonthConfirmedCount;
    private BigDecimal thisMonthConfirmedAmount;
    private Integer rejectedCount;
    private Integer cancelledCount;

    private BigDecimal confirmedCostDelta;
    private BigDecimal projectedContractAmount;
    private BigDecimal projectedGrossProfit;
    private BigDecimal projectedGrossMargin;
    private Boolean costVisible;

    private Integer overdueApprovalCount;
    private Integer overdueConfirmCount;
    private Integer materialPendingCount;
    private Integer materialOrderedCount;
    private Integer costIncompleteCount;

    private Boolean receivableGenerated;
    private Integer receivableOverdueCount;
    private BigDecimal receivableChangeTotal;
    private BigDecimal receivableReceivedTotal;
    private BigDecimal receivableOutstandingTotal;
    private BigDecimal receivableOverdueTotal;

    private List<BossContractChangePeriod> periods = List.of();
    private List<BossContractChangeGroup> groups = List.of();
    private List<BossContractChangeItem> items = List.of();

    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    public Integer getApprovedCount() { return approvedCount; }
    public void setApprovedCount(Integer approvedCount) { this.approvedCount = approvedCount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public BigDecimal getConfirmedNetAmount() { return confirmedNetAmount; }
    public void setConfirmedNetAmount(BigDecimal confirmedNetAmount) { this.confirmedNetAmount = confirmedNetAmount; }
    public Integer getThisMonthConfirmedCount() { return thisMonthConfirmedCount; }
    public void setThisMonthConfirmedCount(Integer thisMonthConfirmedCount) { this.thisMonthConfirmedCount = thisMonthConfirmedCount; }
    public BigDecimal getThisMonthConfirmedAmount() { return thisMonthConfirmedAmount; }
    public void setThisMonthConfirmedAmount(BigDecimal thisMonthConfirmedAmount) { this.thisMonthConfirmedAmount = thisMonthConfirmedAmount; }
    public Integer getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(Integer rejectedCount) { this.rejectedCount = rejectedCount; }
    public Integer getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(Integer cancelledCount) { this.cancelledCount = cancelledCount; }
    public BigDecimal getConfirmedCostDelta() { return confirmedCostDelta; }
    public void setConfirmedCostDelta(BigDecimal confirmedCostDelta) { this.confirmedCostDelta = confirmedCostDelta; }
    public BigDecimal getProjectedContractAmount() { return projectedContractAmount; }
    public void setProjectedContractAmount(BigDecimal projectedContractAmount) { this.projectedContractAmount = projectedContractAmount; }
    public BigDecimal getProjectedGrossProfit() { return projectedGrossProfit; }
    public void setProjectedGrossProfit(BigDecimal projectedGrossProfit) { this.projectedGrossProfit = projectedGrossProfit; }
    public BigDecimal getProjectedGrossMargin() { return projectedGrossMargin; }
    public void setProjectedGrossMargin(BigDecimal projectedGrossMargin) { this.projectedGrossMargin = projectedGrossMargin; }
    public Boolean getCostVisible() { return costVisible; }
    public void setCostVisible(Boolean costVisible) { this.costVisible = costVisible; }
    public Integer getOverdueApprovalCount() { return overdueApprovalCount; }
    public void setOverdueApprovalCount(Integer overdueApprovalCount) { this.overdueApprovalCount = overdueApprovalCount; }
    public Integer getOverdueConfirmCount() { return overdueConfirmCount; }
    public void setOverdueConfirmCount(Integer overdueConfirmCount) { this.overdueConfirmCount = overdueConfirmCount; }
    public Integer getMaterialPendingCount() { return materialPendingCount; }
    public void setMaterialPendingCount(Integer materialPendingCount) { this.materialPendingCount = materialPendingCount; }
    public Integer getMaterialOrderedCount() { return materialOrderedCount; }
    public void setMaterialOrderedCount(Integer materialOrderedCount) { this.materialOrderedCount = materialOrderedCount; }
    public Integer getCostIncompleteCount() { return costIncompleteCount; }
    public void setCostIncompleteCount(Integer costIncompleteCount) { this.costIncompleteCount = costIncompleteCount; }
    public Boolean getReceivableGenerated() { return receivableGenerated; }
    public void setReceivableGenerated(Boolean receivableGenerated) { this.receivableGenerated = receivableGenerated; }
    public Integer getReceivableOverdueCount() { return receivableOverdueCount; }
    public void setReceivableOverdueCount(Integer receivableOverdueCount) { this.receivableOverdueCount = receivableOverdueCount; }
    public BigDecimal getReceivableChangeTotal() { return receivableChangeTotal; }
    public void setReceivableChangeTotal(BigDecimal receivableChangeTotal) { this.receivableChangeTotal = receivableChangeTotal; }
    public BigDecimal getReceivableReceivedTotal() { return receivableReceivedTotal; }
    public void setReceivableReceivedTotal(BigDecimal receivableReceivedTotal) { this.receivableReceivedTotal = receivableReceivedTotal; }
    public BigDecimal getReceivableOutstandingTotal() { return receivableOutstandingTotal; }
    public void setReceivableOutstandingTotal(BigDecimal receivableOutstandingTotal) { this.receivableOutstandingTotal = receivableOutstandingTotal; }
    public BigDecimal getReceivableOverdueTotal() { return receivableOverdueTotal; }
    public void setReceivableOverdueTotal(BigDecimal receivableOverdueTotal) { this.receivableOverdueTotal = receivableOverdueTotal; }
    public List<BossContractChangePeriod> getPeriods() { return periods; }
    public void setPeriods(List<BossContractChangePeriod> periods) {
        this.periods = periods == null ? List.of() : List.copyOf(periods);
    }
    public List<BossContractChangeGroup> getGroups() { return groups; }
    public void setGroups(List<BossContractChangeGroup> groups) {
        this.groups = groups == null ? List.of() : List.copyOf(groups);
    }
    public List<BossContractChangeItem> getItems() { return items; }
    public void setItems(List<BossContractChangeItem> items) {
        this.items = items == null ? List.of() : List.copyOf(items);
    }
}
