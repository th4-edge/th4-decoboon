package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/**
 * 结算预演行（可结算采购明细投影）。
 *
 * <p>已结算明细不会出现在本投影中（互斥 + 月结兜底，DEC-073）；未定价明细的
 * {@code suggestedSettlementPrice} 为空，需财务按对账结果填入结算价。</p>
 */
@Note("主材结算预演行")
public class SettlementPreviewRow {

    @Note("采购明细索引") private Long purchaseItemId;
    @Note("采购单索引") private Long purchaseOrderId;
    @Note("采购单号") private String purchaseNo;
    @Note("供应商索引") private Long supplierId;
    @Note("供应商名称") private String supplierName;
    @Note("材料名称") private String materialName;
    @Note("采购数量") private java.math.BigDecimal quantity;
    @Note("已到场数量") private java.math.BigDecimal receivedQuantity;
    @Note("采购价") private java.math.BigDecimal purchasePrice;
    @Note("建议结算价（未定价为空，需人工填）") private java.math.BigDecimal suggestedSettlementPrice;
    @Note("是否未定价（暂估应付）") private boolean unpriced;

    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(java.math.BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public java.math.BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(java.math.BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public java.math.BigDecimal getSuggestedSettlementPrice() { return suggestedSettlementPrice; }
    public void setSuggestedSettlementPrice(java.math.BigDecimal suggestedSettlementPrice) { this.suggestedSettlementPrice = suggestedSettlementPrice; }
    public boolean isUnpriced() { return unpriced; }
    public void setUnpriced(boolean unpriced) { this.unpriced = unpriced; }
}
