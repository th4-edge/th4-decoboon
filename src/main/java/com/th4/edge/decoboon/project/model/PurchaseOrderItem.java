package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 采购单明细行。
 *
 * <p>一张采购单对应一个供应商，可按供应商合并多条材料需求（多供应商拆单）；
 * 同一条材料需求也可拆给多个供应商，由多条明细承载。</p>
 */
@Note("采购单明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_purchase_order_item", indexes = {
        @Index(name = "idx_purchase_item_company", columnList = "com_id, purchase_item_id"),
        @Index(name = "idx_purchase_item_company_order", columnList = "com_id, purchase_order_id, purchase_item_id"),
        @Index(name = "idx_purchase_item_company_plan", columnList = "com_id, material_plan_id, purchase_item_id"),
        @Index(name = "idx_purchase_item_company_store", columnList = "com_id, store_id, purchase_item_id"),
        @Index(name = "idx_purchase_item_company_team", columnList = "com_id, team_id, purchase_item_id"),
        @Index(name = "idx_purchase_item_company_owner", columnList = "com_id, owner_id, purchase_item_id")
})
public class PurchaseOrderItem extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("采购明细索引")
    private Long purchaseItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("所属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("经营负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    @Note("所属合同")
    private ProjectContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @Note("所属采购单")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_plan_id", nullable = false)
    @Note("来源材料计划")
    private MaterialPlan materialPlan;

    @Column(precision = 12, scale = 3, nullable = false)
    @Note("采购数量")
    private BigDecimal quantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Note("单位")
    private Unit unit = Unit.Area;

    @Column(precision = 12, scale = 2, nullable = false)
    @Note("采购单价")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2, nullable = false)
    @Note("采购金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3, nullable = false)
    @Note("已到场数量")
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("明细状态")
    private PurchaseStatus status = PurchaseStatus.Ordered;

    @Column(length = 32)
    @Note("价格来源：供应关系价/历史成交价/手工录入")
    private String priceSource;

    @Column(length = 500)
    @Note("数量/价格调整原因")
    private String remark;

    public Long getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Long purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public MaterialPlan getMaterialPlan() { return materialPlan; }
    public void setMaterialPlan(MaterialPlan materialPlan) { this.materialPlan = materialPlan; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public String getPriceSource() { return priceSource; }
    public void setPriceSource(String priceSource) { this.priceSource = priceSource; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
