package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 材料采购订单。 */
@Note("采购订单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_purchase_order", indexes = {
        @Index(name = "idx_purchase_company", columnList = "com_id, purchase_id"),
        @Index(name = "idx_purchase_company_project", columnList = "com_id, project_id, purchase_id"),
        @Index(name = "idx_purchase_company_property", columnList = "com_id, property_id, purchase_id")
,
        @Index(name = "idx_purchase_order_company", columnList = "com_id, purchase_id"),
        @Index(name = "idx_purchase_order_company_store", columnList = "com_id, store_id, purchase_id"),
        @Index(name = "idx_purchase_order_company_team", columnList = "com_id, team_id, purchase_id"),
        @Index(name = "idx_purchase_order_company_owner", columnList = "com_id, owner_id, purchase_id")
})
public class PurchaseOrder extends AuditModel implements ProjectOwned {
    @Id @SnowflakeId @Note("采购索引") private Long purchaseId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="com_id",nullable=false) @Note("所属企业") private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="property_id",nullable=false) @Note("所属房屋") private Property property;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="project_id",nullable=false) @Note("所属项目") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="contract_id",nullable=false) @Note("所属合同") private ProjectContract contract;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="material_plan_id",nullable=false) @Note("来源材料计划") private MaterialPlan materialPlan;
    @Column(length=64,nullable=false,unique=true) @Note("采购单号") private String no;
    @Column(length=128,nullable=false) @Note("供应商名称") private String supplierName;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="supplier_id") @Note("供应商") private Partner supplier;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="quote_line_id") @Note("来源报价行") private QuoteLine quoteLine;
    @Enumerated(EnumType.STRING) @Column(name="delivery_mode", length=16, nullable=false) @Note("交付方式") private DeliveryMode deliveryMode=DeliveryMode.Direct;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="warehouse_id") @Note("入库仓库") private Store warehouse;
    @Column(precision=12,scale=3,nullable=false) @Note("采购数量") private BigDecimal quantity=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(length=32,nullable=false) @Note("单位") private Unit unit=Unit.Area;
    @Column(precision=12,scale=2,nullable=false) @Note("采购单价") private BigDecimal unitPrice=BigDecimal.ZERO;
    @Column(precision=14,scale=2,nullable=false) @Note("采购金额") private BigDecimal amount=BigDecimal.ZERO;
    @Column(precision=12,scale=3,nullable=false) @Note("已到场数量") private BigDecimal receivedQuantity=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(length=24,nullable=false) @Note("采购状态") private PurchaseStatus status=PurchaseStatus.Submitted;
    @Note("要求到场日期") private LocalDate expectedAt;
    @Note("实际下单日期") private LocalDate orderedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="creator_id",nullable=false) @Note("创建人") private Employee createdBy;
    @Column(length=500) @Note("备注") private String remark;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="approver_id") @Note("审批人") private Employee approvedBy;
    @Note("审批时间") private LocalDateTime approvedAt;
    @Column(length=500) @Note("审批意见") private String approvalRemark;
    public Long getPurchaseId(){return purchaseId;} public void setPurchaseId(Long v){purchaseId=v;}
    public Company getCompany(){return company;} public void setCompany(Company v){company=v;}
    public Property getProperty(){return property;} public void setProperty(Property v){property=v;}
    public Project getProject(){return project;} public void setProject(Project v){project=v;}
    public ProjectContract getContract(){return contract;} public void setContract(ProjectContract v){contract=v;}
    public MaterialPlan getMaterialPlan(){return materialPlan;} public void setMaterialPlan(MaterialPlan v){materialPlan=v;}
    public String getNo(){return no;} public void setNo(String v){no=v;}
    public String getSupplierName(){return supplierName;} public void setSupplierName(String v){supplierName=v;}
    public Partner getSupplier(){return supplier;} public void setSupplier(Partner v){supplier=v;}
    public QuoteLine getQuoteLine(){return quoteLine;} public void setQuoteLine(QuoteLine v){quoteLine=v;}
    public DeliveryMode getDeliveryMode(){return deliveryMode;} public void setDeliveryMode(DeliveryMode v){deliveryMode=v;}
    public Store getWarehouse(){return warehouse;} public void setWarehouse(Store v){warehouse=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public Unit getUnit(){return unit;} public void setUnit(Unit v){unit=v;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public BigDecimal getReceivedQuantity(){return receivedQuantity;} public void setReceivedQuantity(BigDecimal v){receivedQuantity=v;}
    public PurchaseStatus getStatus(){return status;} public void setStatus(PurchaseStatus v){status=v;}
    public LocalDate getExpectedAt(){return expectedAt;} public void setExpectedAt(LocalDate v){expectedAt=v;}
    public LocalDate getOrderedAt(){return orderedAt;} public void setOrderedAt(LocalDate v){orderedAt=v;}
    public Employee getCreatedBy(){return createdBy;} public void setCreatedBy(Employee v){createdBy=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public Employee getApprovedBy(){return approvedBy;} public void setApprovedBy(Employee v){approvedBy=v;}
    public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime v){approvedAt=v;}
    public String getApprovalRemark(){return approvalRemark;} public void setApprovalRemark(String v){approvalRemark=v;}
    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public Employee getOwner() { return owner; }

    @Override
    public void setOwner(Employee owner) { this.owner = owner; }


}
