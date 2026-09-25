package com.th4.edge.decoboon.catalog.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.model.Company;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 报价项目库导入行：预览、错误清单与导入来源的最小审计单位（PRD §11.1）。 */
@Note("报价项目库导入行")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_import_row", indexes = {
        @Index(name = "idx_catalog_import_row_company", columnList = "com_id, row_id"),
        @Index(name = "idx_catalog_import_row_batch_status", columnList = "com_id, batch_id, status, row_no"),
        @Index(name = "idx_catalog_import_row_item", columnList = "com_id, item_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_catalog_import_row_batch_no", columnNames = {"com_id", "batch_id", "row_no"})
})
public class CatalogImportRow extends AbstractModel {

    @Note("导入行索引")
    @Id
    @SnowflakeId
    private Long rowId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属批次")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CatalogImportBatch batch;

    @Note("Excel/CSV 行号（含表头，数据从第 2 行开始）")
    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    @Note("行状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private CatalogImportRowStatus status = CatalogImportRowStatus.Ready;

    @Note("错误/跳过字段")
    @Column(length = 64)
    private String errorField;

    @Note("错误码")
    @Column(length = 64)
    private String errorCode;

    @Note("错误/跳过原因")
    @Column(length = 512)
    private String errorReason;

    @Note("行原始数据快照")
    @Lob
    private String rawData;

    @Note("项目名称（预览摘要）")
    @Column(name = "item_name", length = 256)
    private String itemName;

    @Note("项目类型（预览摘要）")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20)
    private ItemType itemType;

    @Note("计量单位（预览摘要）")
    @Enumerated(EnumType.STRING)
    @Column(name = "unit", length = 32)
    private Unit unit;

    @Note("计价方式（预览摘要）")
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", length = 24)
    private PricingMethod pricingMode;

    @Note("售价摘要（固定总价项目为固定总价，其余为材料+人工）")
    @Column(name = "sale_amount", precision = 12, scale = 2)
    private BigDecimal saleAmount;

    @Note("成本是否待维护（预览摘要，非授权角色不返回）")
    @Column(name = "cost_pending")
    private Boolean costPending = Boolean.FALSE;

    @Note("导入成功后关联的项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private CatalogItem item;

    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CatalogImportBatch getBatch() { return batch; }
    public void setBatch(CatalogImportBatch batch) { this.batch = batch; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public CatalogImportRowStatus getStatus() { return status; }
    public void setStatus(CatalogImportRowStatus status) { this.status = status; }
    public String getErrorField() { return errorField; }
    public void setErrorField(String errorField) { this.errorField = errorField; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public PricingMethod getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMethod pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getSaleAmount() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount = saleAmount; }
    public Boolean getCostPending() { return costPending; }
    public void setCostPending(Boolean costPending) { this.costPending = costPending; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
}
