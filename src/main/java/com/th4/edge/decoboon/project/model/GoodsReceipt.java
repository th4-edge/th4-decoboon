package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
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
import java.time.LocalDateTime;
import java.util.List;

/** 材料到场验收。 */
@Note("材料到场")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_goods_receipt", indexes = {
        @Index(name="idx_goods_receipt_company", columnList="com_id, receipt_id"),
        @Index(name="idx_goods_receipt_purchase", columnList="com_id, purchase_id, receipt_id")
,
        @Index(name = "idx_goods_receipt_company", columnList = "com_id, receipt_id"),
        @Index(name = "idx_goods_receipt_company_store", columnList = "com_id, store_id, receipt_id"),
        @Index(name = "idx_goods_receipt_company_team", columnList = "com_id, team_id, receipt_id"),
        @Index(name = "idx_goods_receipt_company_owner", columnList = "com_id, owner_id, receipt_id")
})
public class GoodsReceipt extends AuditModel implements ProjectOwned {
    @Id @SnowflakeId @Note("到场索引") private Long receiptId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="com_id",nullable=false) @Note("所属企业") private Company company;

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

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id",nullable=false) @Note("所属房屋") private Property property;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="project_id",nullable=false) @Note("所属项目") private Project project;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="purchase_id",nullable=false) @Note("采购订单") private PurchaseOrder purchase;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="material_plan_id",nullable=false) @Note("材料计划") private MaterialPlan materialPlan;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="purchase_item_id") @Note("采购明细") private PurchaseOrderItem purchaseItem;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="warehouse_id") @Note("入库仓库") private Store warehouse;
    @Column(precision=12,scale=3,nullable=false) @Note("到场数量") private BigDecimal quantity=BigDecimal.ZERO;
    @Column(length=64) @Note("批次号") private String batchNo;
    @Note("有效期") private java.time.LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @Column(length=32,nullable=false) @Note("单位") private Unit unit=Unit.Area;
    @Note("到场时间") private LocalDateTime arrivedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="inspector_id",nullable=false) @Note("验收人") private Employee inspector;
    @ElementCollection @CollectionTable(name="db_goods_receipt_images",joinColumns=@JoinColumn(name="receipt_id")) @Column(name="url",length=512) private List<String> images;
    @Column(length=500) @Note("备注") private String remark;
    public Long getReceiptId(){return receiptId;} public void setReceiptId(Long v){receiptId=v;}
    public Company getCompany(){return company;} public void setCompany(Company v){company=v;}
    public Property getProperty(){return property;} public void setProperty(Property v){property=v;}
    public Project getProject(){return project;} public void setProject(Project v){project=v;}
    public PurchaseOrder getPurchase(){return purchase;} public void setPurchase(PurchaseOrder v){purchase=v;}
    public MaterialPlan getMaterialPlan(){return materialPlan;} public void setMaterialPlan(MaterialPlan v){materialPlan=v;}
    public PurchaseOrderItem getPurchaseItem(){return purchaseItem;} public void setPurchaseItem(PurchaseOrderItem v){purchaseItem=v;}
    public Store getWarehouse(){return warehouse;} public void setWarehouse(Store v){warehouse=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public String getBatchNo(){return batchNo;} public void setBatchNo(String v){batchNo=v;}
    public java.time.LocalDate getExpiryDate(){return expiryDate;} public void setExpiryDate(java.time.LocalDate v){expiryDate=v;}
    public Unit getUnit(){return unit;} public void setUnit(Unit v){unit=v;}
    public LocalDateTime getArrivedAt(){return arrivedAt;} public void setArrivedAt(LocalDateTime v){arrivedAt=v;}
    public Employee getInspector(){return inspector;} public void setInspector(Employee v){inspector=v;}
    public List<String> getImages(){return images;} public void setImages(List<String> v){images=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
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
