package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 材料计划工作台行：材料需求 + 选材结果 + 下单/到场进度。 */
@Note("材料计划工作台行")
public class MaterialPlanWorkbenchItem {

    @Note("计划行索引") private Long planId;
    @Note("项目索引") private Long projectId;
    @Note("项目名称") private String projectName;
    @Note("房屋地址") private String propertyAddress;
    @Note("来源类型值") private String sourceType;
    @Note("来源类型名称") private String sourceTypeName;
    @Note("空间") private String spaceName;
    @Note("材料分类") private String category;
    @Note("材料名称") private String materialName;
    @Note("品牌") private String brand;
    @Note("规格") private String spec;
    @Note("型号") private String model;
    @Note("单位") private String unit;
    @Note("净用量") private BigDecimal netQuantity;
    @Note("损耗率") private BigDecimal lossRate;
    @Note("损耗量") private BigDecimal lossQuantity;
    @Note("需求数量（净用量+损耗量）") private BigDecimal quantity;
    @Note("已下单数量") private BigDecimal orderedQuantity;
    @Note("已到场数量") private BigDecimal receivedQuantity;
    @Note("要求到场日期") private LocalDate requiredAt;
    @Note("实际到场日期") private LocalDate arrivedAt;
    @Note("计划状态值") private String status;
    @Note("计划状态名称") private String statusName;
    @Note("选材记录索引") private Long selectionId;
    @Note("选材状态值") private String selectionStatus;
    @Note("选材状态名称") private String selectionStatusName;
    @Note("最新采购单索引") private Long purchaseId;
    @Note("最新采购明细索引") private Long purchaseItemId;
    @Note("最新采购单号") private String purchaseNo;
    @Note("供应商名称") private String supplierName;
    @Note("是否已逾期") private boolean delayed;
    @Note("下一步动作") private String nextAction;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceTypeName() { return sourceTypeName; }
    public void setSourceTypeName(String sourceTypeName) { this.sourceTypeName = sourceTypeName; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getNetQuantity() { return netQuantity; }
    public void setNetQuantity(BigDecimal netQuantity) { this.netQuantity = netQuantity; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public BigDecimal getLossQuantity() { return lossQuantity; }
    public void setLossQuantity(BigDecimal lossQuantity) { this.lossQuantity = lossQuantity; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(BigDecimal orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public LocalDate getRequiredAt() { return requiredAt; }
    public void setRequiredAt(LocalDate requiredAt) { this.requiredAt = requiredAt; }
    public LocalDate getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(LocalDate arrivedAt) { this.arrivedAt = arrivedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Long getSelectionId() { return selectionId; }
    public void setSelectionId(Long selectionId) { this.selectionId = selectionId; }
    public String getSelectionStatus() { return selectionStatus; }
    public void setSelectionStatus(String selectionStatus) { this.selectionStatus = selectionStatus; }
    public String getSelectionStatusName() { return selectionStatusName; }
    public void setSelectionStatusName(String selectionStatusName) { this.selectionStatusName = selectionStatusName; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public boolean isDelayed() { return delayed; }
    public void setDelayed(boolean delayed) { this.delayed = delayed; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
}
