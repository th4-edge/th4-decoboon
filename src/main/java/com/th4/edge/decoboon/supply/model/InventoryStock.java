package com.th4.edge.decoboon.supply.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 主材批次库存。 */
@Note("库存")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_decoboon_inventory_stock", indexes = {
        @Index(name = "idx_stock_company", columnList = "com_id, stock_id"),
        @Index(name = "idx_stock_company_store", columnList = "com_id, store_id, stock_id"),
        @Index(name = "idx_stock_company_item", columnList = "com_id, item_id, stock_id")
})
public class InventoryStock extends AuditModel {

    @Id @SnowflakeId @Note("库存索引") private Long stockId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") @Note("仓库门店") private Store store;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false) @Note("项目库") private CatalogItem item;
    @Column(length = 64) @Note("批次号") private String batchNo;
    @Column(precision = 14, scale = 3, nullable = false) @Note("库存数量") private BigDecimal quantity = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3, nullable = false) @Note("锁定数量") private BigDecimal lockedQuantity = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("单位成本") private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3, nullable = false) @Note("安全库存") private BigDecimal safetyStock = BigDecimal.ZERO;
    @Note("有效期") private LocalDate expiryDate;
    @Note("最近入库时间") private java.time.LocalDateTime lastInAt;
    @Note("最近出库时间") private java.time.LocalDateTime lastOutAt;
    @Transient @Note("可用数量") private BigDecimal availableQuantity;

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getLockedQuantity() { return lockedQuantity; }
    public void setLockedQuantity(BigDecimal lockedQuantity) { this.lockedQuantity = lockedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(BigDecimal safetyStock) { this.safetyStock = safetyStock; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public java.time.LocalDateTime getLastInAt() { return lastInAt; }
    public void setLastInAt(java.time.LocalDateTime lastInAt) { this.lastInAt = lastInAt; }
    public java.time.LocalDateTime getLastOutAt() { return lastOutAt; }
    public void setLastOutAt(java.time.LocalDateTime lastOutAt) { this.lastOutAt = lastOutAt; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }
}
