package com.th4.edge.decoboon.supply.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.supply.configure.StockMovementType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 库存出入、损耗和退料流水。 */
@Note("库存流水")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_stock_movement", indexes = {
        @Index(name = "idx_stock_movement_company", columnList = "com_id, movement_id"),
        @Index(name = "idx_stock_movement_project", columnList = "com_id, project_id, movement_id"),
        @Index(name = "idx_stock_movement_stock", columnList = "com_id, stock_id, movement_id")
})
public class StockMovement extends AuditModel {

    @Id @SnowflakeId @Note("流水索引") private Long movementId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stock_id", nullable = false) @Note("库存批次") private InventoryStock stock;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") @Note("仓库门店") private Store store;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false) @Note("项目库") private CatalogItem item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") @Note("项目") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quote_line_id") @Note("报价行") private QuoteLine quoteLine;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "material_plan_id") @Note("材料计划") private MaterialPlan materialPlan;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "purchase_id") @Note("采购单") private PurchaseOrder purchase;
    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) @Note("变动类型") private StockMovementType type;
    @Column(precision = 14, scale = 3, nullable = false) @Note("变动数量") private BigDecimal quantity = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("单位成本") private BigDecimal unitCost = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("成本金额") private BigDecimal amount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3, nullable = false) @Note("变动前数量") private BigDecimal beforeQuantity = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3, nullable = false) @Note("变动后数量") private BigDecimal afterQuantity = BigDecimal.ZERO;
    @Column(length = 64) @Note("来源单号") private String sourceNo;
    @Note("发生时间") private LocalDateTime occurredAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id", nullable = false) @Note("操作人") private Employee operator;
    @ElementCollection @CollectionTable(name = "db_stock_movement_image", joinColumns = @JoinColumn(name = "movement_id")) @Column(name = "url", length = 512) private List<String> images;
    @Column(length = 500) @Note("备注") private String remark;

    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public InventoryStock getStock() { return stock; }
    public void setStock(InventoryStock stock) { this.stock = stock; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public QuoteLine getQuoteLine() { return quoteLine; }
    public void setQuoteLine(QuoteLine quoteLine) { this.quoteLine = quoteLine; }
    public MaterialPlan getMaterialPlan() { return materialPlan; }
    public void setMaterialPlan(MaterialPlan materialPlan) { this.materialPlan = materialPlan; }
    public PurchaseOrder getPurchase() { return purchase; }
    public void setPurchase(PurchaseOrder purchase) { this.purchase = purchase; }
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type = type; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBeforeQuantity() { return beforeQuantity; }
    public void setBeforeQuantity(BigDecimal beforeQuantity) { this.beforeQuantity = beforeQuantity; }
    public BigDecimal getAfterQuantity() { return afterQuantity; }
    public void setAfterQuantity(BigDecimal afterQuantity) { this.afterQuantity = afterQuantity; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
