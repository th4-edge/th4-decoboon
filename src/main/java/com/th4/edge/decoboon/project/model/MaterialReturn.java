package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.MaterialReturnStatus;
import com.th4.edge.decoboon.supply.model.Partner;
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
import java.time.LocalDate;

/**
 * 材料退货单。
 *
 * <p>DEC-MP-02：已到货材料的扣减必须走退货单，再冲减应付与已确认成本，
 * 禁止直接修改应付金额。退货来源为合同变更减项或财务手工发起。</p>
 */
@Note("材料退货单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_return", indexes = {
        @Index(name = "idx_material_return_company", columnList = "com_id, material_return_id"),
        @Index(name = "idx_material_return_company_project", columnList = "com_id, project_id, material_return_id"),
        @Index(name = "idx_material_return_company_plan", columnList = "com_id, material_plan_id, material_return_id"),
        @Index(name = "idx_material_return_company_purchase", columnList = "com_id, purchase_order_id, material_return_id"),
        @Index(name = "idx_material_return_company_store", columnList = "com_id, store_id, material_return_id"),
        @Index(name = "idx_material_return_company_team", columnList = "com_id, team_id, material_return_id"),
        @Index(name = "idx_material_return_company_owner", columnList = "com_id, owner_id, material_return_id")
})
public class MaterialReturn extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("退货单索引")
    private Long materialReturnId;

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
    @JoinColumn(name = "material_plan_id", nullable = false)
    @Note("来源材料计划")
    private MaterialPlan materialPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    @Note("来源采购单")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    @Note("供应商")
    private Partner supplier;

    @Column(length = 64)
    @Note("退货单号")
    private String no;

    @Column(precision = 12, scale = 3, nullable = false)
    @Note("退货数量")
    private BigDecimal quantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Note("单位")
    private Unit unit = Unit.Area;

    @Column(precision = 12, scale = 2, nullable = false)
    @Note("退货单价")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2, nullable = false)
    @Note("退货金额（冲减应付与成本）")
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("退货状态")
    private MaterialReturnStatus status = MaterialReturnStatus.Confirmed;

    @Note("退货日期")
    private LocalDate returnedAt;

    @Column(length = 500)
    @Note("退货原因/来源说明")
    private String reason;

    @Column(length = 500)
    @Note("备注（含待协商说明）")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @Note("创建人")
    private Employee createdBy;

    public Long getMaterialReturnId() { return materialReturnId; }
    public void setMaterialReturnId(Long materialReturnId) { this.materialReturnId = materialReturnId; }
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
    public MaterialPlan getMaterialPlan() { return materialPlan; }
    public void setMaterialPlan(MaterialPlan materialPlan) { this.materialPlan = materialPlan; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public Partner getSupplier() { return supplier; }
    public void setSupplier(Partner supplier) { this.supplier = supplier; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public MaterialReturnStatus getStatus() { return status; }
    public void setStatus(MaterialReturnStatus status) { this.status = status; }
    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
