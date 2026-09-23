package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业负责人增减项单条经营投影。 */
@Note("企业负责人增减项明细")
public class BossContractChangeItem {

    private Long changeId;
    private String changeNo;
    private String status;
    private String statusName;
    private String type;
    private String typeName;

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
    private BigDecimal projectedContractAmount;
    private BigDecimal amount;

    private BigDecimal costDelta;
    private BigDecimal projectedGrossProfit;
    private BigDecimal projectedGrossMargin;
    private Boolean costComplete;

    private Integer itemCount;
    private Integer constructionItemCount;
    private Integer materialItemCount;
    private Integer materialPendingCount;
    private Integer materialOrderedCount;

    private Long requestedById;
    private String requestedByName;
    private Long approvedById;
    private String approvedByName;

    private LocalDateTime createTime;
    private LocalDateTime approvedAt;
    private LocalDateTime customerConfirmedAt;
    private Long waitingHours;

    private String riskLevel;
    private String riskText;
    private String nextAction;
    private String lineSummary;
    private Integer evidenceImageCount;
    private Boolean hasSignature;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }
    public String getChangeNo() { return changeNo; }
    public void setChangeNo(String changeNo) { this.changeNo = changeNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
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
    public BigDecimal getProjectedContractAmount() { return projectedContractAmount; }
    public void setProjectedContractAmount(BigDecimal projectedContractAmount) { this.projectedContractAmount = projectedContractAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCostDelta() { return costDelta; }
    public void setCostDelta(BigDecimal costDelta) { this.costDelta = costDelta; }
    public BigDecimal getProjectedGrossProfit() { return projectedGrossProfit; }
    public void setProjectedGrossProfit(BigDecimal projectedGrossProfit) { this.projectedGrossProfit = projectedGrossProfit; }
    public BigDecimal getProjectedGrossMargin() { return projectedGrossMargin; }
    public void setProjectedGrossMargin(BigDecimal projectedGrossMargin) { this.projectedGrossMargin = projectedGrossMargin; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getConstructionItemCount() { return constructionItemCount; }
    public void setConstructionItemCount(Integer constructionItemCount) { this.constructionItemCount = constructionItemCount; }
    public Integer getMaterialItemCount() { return materialItemCount; }
    public void setMaterialItemCount(Integer materialItemCount) { this.materialItemCount = materialItemCount; }
    public Integer getMaterialPendingCount() { return materialPendingCount; }
    public void setMaterialPendingCount(Integer materialPendingCount) { this.materialPendingCount = materialPendingCount; }
    public Integer getMaterialOrderedCount() { return materialOrderedCount; }
    public void setMaterialOrderedCount(Integer materialOrderedCount) { this.materialOrderedCount = materialOrderedCount; }
    public Long getRequestedById() { return requestedById; }
    public void setRequestedById(Long requestedById) { this.requestedById = requestedById; }
    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }
    public Long getApprovedById() { return approvedById; }
    public void setApprovedById(Long approvedById) { this.approvedById = approvedById; }
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCustomerConfirmedAt() { return customerConfirmedAt; }
    public void setCustomerConfirmedAt(LocalDateTime customerConfirmedAt) { this.customerConfirmedAt = customerConfirmedAt; }
    public Long getWaitingHours() { return waitingHours; }
    public void setWaitingHours(Long waitingHours) { this.waitingHours = waitingHours; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskText() { return riskText; }
    public void setRiskText(String riskText) { this.riskText = riskText; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getLineSummary() { return lineSummary; }
    public void setLineSummary(String lineSummary) { this.lineSummary = lineSummary; }
    public Integer getEvidenceImageCount() { return evidenceImageCount; }
    public void setEvidenceImageCount(Integer evidenceImageCount) { this.evidenceImageCount = evidenceImageCount; }
    public Boolean getHasSignature() { return hasSignature; }
    public void setHasSignature(Boolean hasSignature) { this.hasSignature = hasSignature; }
}
