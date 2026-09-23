package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 企业负责人增减项按项目聚合分组。 */
@Note("企业负责人增减项项目分组")
public class BossContractChangeGroup {

    private Long projectId;
    private String projectNo;
    private String projectName;
    private String projectStatus;
    private String customerName;
    private String community;
    private String building;
    private String roomNo;
    private String address;

    private Long contractId;
    private String contractNo;
    private BigDecimal contractCurrentAmount;
    private BigDecimal inFlightAmount;
    private BigDecimal projectedContractAmount;

    private Integer totalCount;
    private Integer pendingCount;
    private Integer approvedCount;
    private Integer confirmedCount;
    private BigDecimal pendingAmount;
    private BigDecimal confirmedNetAmount;
    private BigDecimal costDelta;
    private BigDecimal projectedGrossProfit;
    private BigDecimal projectedGrossMargin;
    private Boolean costComplete;

    private Integer materialPendingCount;
    private Integer materialOrderedCount;
    private Integer riskCount;
    private String riskLevel;
    private String riskText;

    private Boolean receivableGenerated;
    private BigDecimal receivableTotal;
    private BigDecimal receivableReceivedTotal;
    private BigDecimal receivableOutstandingTotal;
    private BigDecimal receivableOverdueTotal;
    private Integer receivableOverdueCount;

    private List<BossContractChangeItem> items = List.of();

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public BigDecimal getContractCurrentAmount() { return contractCurrentAmount; }
    public void setContractCurrentAmount(BigDecimal contractCurrentAmount) { this.contractCurrentAmount = contractCurrentAmount; }
    public BigDecimal getInFlightAmount() { return inFlightAmount; }
    public void setInFlightAmount(BigDecimal inFlightAmount) { this.inFlightAmount = inFlightAmount; }
    public BigDecimal getProjectedContractAmount() { return projectedContractAmount; }
    public void setProjectedContractAmount(BigDecimal projectedContractAmount) { this.projectedContractAmount = projectedContractAmount; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public Integer getApprovedCount() { return approvedCount; }
    public void setApprovedCount(Integer approvedCount) { this.approvedCount = approvedCount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    public BigDecimal getConfirmedNetAmount() { return confirmedNetAmount; }
    public void setConfirmedNetAmount(BigDecimal confirmedNetAmount) { this.confirmedNetAmount = confirmedNetAmount; }
    public BigDecimal getCostDelta() { return costDelta; }
    public void setCostDelta(BigDecimal costDelta) { this.costDelta = costDelta; }
    public BigDecimal getProjectedGrossProfit() { return projectedGrossProfit; }
    public void setProjectedGrossProfit(BigDecimal projectedGrossProfit) { this.projectedGrossProfit = projectedGrossProfit; }
    public BigDecimal getProjectedGrossMargin() { return projectedGrossMargin; }
    public void setProjectedGrossMargin(BigDecimal projectedGrossMargin) { this.projectedGrossMargin = projectedGrossMargin; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public Integer getMaterialPendingCount() { return materialPendingCount; }
    public void setMaterialPendingCount(Integer materialPendingCount) { this.materialPendingCount = materialPendingCount; }
    public Integer getMaterialOrderedCount() { return materialOrderedCount; }
    public void setMaterialOrderedCount(Integer materialOrderedCount) { this.materialOrderedCount = materialOrderedCount; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskText() { return riskText; }
    public void setRiskText(String riskText) { this.riskText = riskText; }
    public Boolean getReceivableGenerated() { return receivableGenerated; }
    public void setReceivableGenerated(Boolean receivableGenerated) { this.receivableGenerated = receivableGenerated; }
    public BigDecimal getReceivableTotal() { return receivableTotal; }
    public void setReceivableTotal(BigDecimal receivableTotal) { this.receivableTotal = receivableTotal; }
    public BigDecimal getReceivableReceivedTotal() { return receivableReceivedTotal; }
    public void setReceivableReceivedTotal(BigDecimal receivableReceivedTotal) { this.receivableReceivedTotal = receivableReceivedTotal; }
    public BigDecimal getReceivableOutstandingTotal() { return receivableOutstandingTotal; }
    public void setReceivableOutstandingTotal(BigDecimal receivableOutstandingTotal) { this.receivableOutstandingTotal = receivableOutstandingTotal; }
    public BigDecimal getReceivableOverdueTotal() { return receivableOverdueTotal; }
    public void setReceivableOverdueTotal(BigDecimal receivableOverdueTotal) { this.receivableOverdueTotal = receivableOverdueTotal; }
    public Integer getReceivableOverdueCount() { return receivableOverdueCount; }
    public void setReceivableOverdueCount(Integer receivableOverdueCount) { this.receivableOverdueCount = receivableOverdueCount; }
    public List<BossContractChangeItem> getItems() { return items; }
    public void setItems(List<BossContractChangeItem> items) {
        this.items = items == null ? List.of() : List.copyOf(items);
    }
}
