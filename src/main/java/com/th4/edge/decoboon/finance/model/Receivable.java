package com.th4.edge.decoboon.finance.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 合同节点应收计划。 */
@Note("应收计划")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_receivable", indexes = {
        @Index(name = "idx_fin_receivable_company", columnList = "com_id, receivable_id"),
        @Index(name = "idx_fin_receivable_project", columnList = "com_id, project_id, receivable_id"),
        @Index(name = "idx_fin_receivable_store", columnList = "com_id, store_id, receivable_id"),
        @Index(name = "idx_fin_receivable_team", columnList = "com_id, team_id, receivable_id"),
        @Index(name = "idx_fin_receivable_owner", columnList = "com_id, owner_id, receivable_id")
})
public class Receivable extends AuditModel {

    @Note("应收索引")
    @Id
    @SnowflakeId
    private Long receivableId;

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
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("来源合同")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ProjectContract contract;

    @Note("关联施工阶段")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private ProjectStage stage;

    @Note("稳定阶段编码")
    @Column(name = "stage_code", length = 64)
    private String stageCode;

    @Note("付款触发类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 24)
    private PaymentTriggerType triggerType;

    @Note("来源付款节点快照")
    @OneToOne(mappedBy = "receivable", fetch = FetchType.LAZY)
    private ContractPaymentNode planNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Note("收款节点")
    @Keyword
    @Column(length = 128, nullable = false)
    private String name;

    @Note("对应施工节点")
    @Column(length = 128)
    private String stageName;

    @Note("应收类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ReceivableType type = ReceivableType.Other;

    @Note("应收金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("已收金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Note("约定收款日")
    @Column(nullable = false)
    private LocalDate dueDate;

    @Note("应收状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ReceivableStatus status = ReceivableStatus.Pending;

    @Note("是否开票")
    @Column(nullable = false)
    private Boolean invoiceRequired = Boolean.FALSE;

    @Note("发票号码")
    @Column(length = 64)
    private String invoiceNo;

    @Note("业务说明")
    @Column(length = 500)
    private String remark;

    @Note("创建人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getReceivableId() { return receivableId; }
    public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
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
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public ReceivableType getType() { return type; }
    public void setType(ReceivableType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public ReceivableStatus getStatus() { return status; }
    public void setStatus(ReceivableStatus status) { this.status = status; }
    public Boolean getInvoiceRequired() { return invoiceRequired; }
    public void setInvoiceRequired(Boolean invoiceRequired) { this.invoiceRequired = invoiceRequired; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public PaymentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(PaymentTriggerType triggerType) { this.triggerType = triggerType; }
    public ContractPaymentNode getPlanNode() { return planNode; }
    public void setPlanNode(ContractPaymentNode planNode) { this.planNode = planNode; }
}
