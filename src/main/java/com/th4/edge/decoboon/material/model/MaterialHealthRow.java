package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 材料业务体检聚焦明细：只保留解释下一步所需字段。 */
@Note("材料业务体检聚焦明细")
public class MaterialHealthRow {

    @Note("材料计划索引")
    private Long planId;
    @Note("空间")
    private String spaceName;
    @Note("材料分类")
    private String category;
    @Note("材料名称")
    private String materialName;
    @Note("计划状态值")
    private String status;
    @Note("计划状态名称")
    private String statusName;
    @Note("选材状态值")
    private String selectionStatus;
    @Note("选材状态名称")
    private String selectionStatusName;
    @Note("需求数量")
    private BigDecimal quantity;
    @Note("已下单数量")
    private BigDecimal orderedQuantity;
    @Note("已到场数量")
    private BigDecimal receivedQuantity;
    @Note("要求到场日期")
    private LocalDate requiredAt;
    @Note("供应商名称")
    private String supplierName;
    @Note("采购单号")
    private String purchaseNo;
    @Note("是否逾期")
    private boolean delayed;
    @Note("下一步动作")
    private String nextAction;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getSelectionStatus() { return selectionStatus; }
    public void setSelectionStatus(String selectionStatus) { this.selectionStatus = selectionStatus; }
    public String getSelectionStatusName() { return selectionStatusName; }
    public void setSelectionStatusName(String selectionStatusName) { this.selectionStatusName = selectionStatusName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(BigDecimal orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public LocalDate getRequiredAt() { return requiredAt; }
    public void setRequiredAt(LocalDate requiredAt) { this.requiredAt = requiredAt; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public boolean isDelayed() { return delayed; }
    public void setDelayed(boolean delayed) { this.delayed = delayed; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
}
