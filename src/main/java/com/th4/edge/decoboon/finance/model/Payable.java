package com.th4.edge.decoboon.finance.model;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.finance.configure.SettlementMode;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 供应商、工人、项目班组或费用形成的付款义务。 */
@Note("应付单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_payable", indexes = {
        @Index(name = "idx_fin_payable_company", columnList = "com_id, payable_id"),
        @Index(name = "idx_fin_payable_project", columnList = "com_id, project_id, payable_id"),
        @Index(name = "idx_fin_payable_store", columnList = "com_id, store_id, payable_id"),
        @Index(name = "idx_fin_payable_team", columnList = "com_id, team_id, payable_id"),
        @Index(name = "idx_fin_payable_owner", columnList = "com_id, owner_id, payable_id")
})
public class Payable extends AuditModel {

    @Note("应付索引")
    @Id
    @SnowflakeId
    private Long payableId;

    @Note("业务来源")
    @Enumerated(EnumType.STRING)
    @Column(name = "business_origin", length = 16, nullable = false)
    private BusinessOrigin businessOrigin = BusinessOrigin.New;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("归属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("归属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("数据负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    @Note("所属房屋；企业归属员工工资可为空")
    private Property property;

    @Note("材料计划来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_plan_id")
    private MaterialPlan materialPlan;

    @Note("来源采购单（下单即形成应付）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Note("应付名称")
    @Keyword
    @Column(length = 128, nullable = false)
    private String name;

    @Note("费用类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PayableType type = PayableType.Other;

    @Note("收款人员工")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_emp_id")
    private Employee payeeEmployee;

    @Note("收款方名称")
    @Keyword
    @Column(length = 128)
    private String payeeName;

    @Note("收款合作方")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_partner_id")
    private Partner payeePartner;

    @Note("账期/工资周期")
    @Column(length = 64)
    private String period;

    @Note("应付金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("已付金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Note("收货日（账期起算）")
    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Note("账期模式快照（下单时固化，避免供应商后续改规则影响历史应付）")
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_mode", length = 16)
    private SettlementMode settlementMode;

    @Note("约定付款日（到场确认后按供应商账期回填）")
    @Column
    private LocalDate dueDate;

    @Note("应付状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PayableStatus status = PayableStatus.Pending;

    @Note("是否加急")
    @Column(nullable = false)
    private Boolean urgent = Boolean.FALSE;

    @Note("来源单号")
    @Column(length = 64)
    private String sourceNo;

    @Note("来源类型：PayrollItem / CommissionItem")
    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Note("来源业务主键")
    @Column(name = "source_id")
    private Long sourceId;

    @Note("业务说明")
    @Column(length = 500)
    private String remark;

    @Note("创建人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

        public BusinessOrigin getBusinessOrigin() { return businessOrigin; }
    public void setBusinessOrigin(BusinessOrigin businessOrigin) { this.businessOrigin = businessOrigin; }
public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public MaterialPlan getMaterialPlan() { return materialPlan; }
    public void setMaterialPlan(MaterialPlan materialPlan) { this.materialPlan = materialPlan; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PayableType getType() { return type; }
    public void setType(PayableType type) { this.type = type; }
    public Employee getPayeeEmployee() { return payeeEmployee; }
    public void setPayeeEmployee(Employee payeeEmployee) { this.payeeEmployee = payeeEmployee; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public Partner getPayeePartner() { return payeePartner; }
    public void setPayeePartner(Partner payeePartner) { this.payeePartner = payeePartner; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public SettlementMode getSettlementMode() { return settlementMode; }
    public void setSettlementMode(SettlementMode settlementMode) { this.settlementMode = settlementMode; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public PayableStatus getStatus() { return status; }
    public void setStatus(PayableStatus status) { this.status = status; }
    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
