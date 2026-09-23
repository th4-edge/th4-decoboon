package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 项目合同基线。 */
@Note("项目合同基线")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project_contract",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_contract_project", columnNames = "project_id"),
                @UniqueConstraint(name = "uk_contract_quote", columnNames = "quote_id"),
                @UniqueConstraint(name = "uk_contract_property", columnNames = "property_id")
        },
        indexes = {
                @Index(name = "idx_contract_company", columnList = "com_id, contract_id"),
                @Index(name = "idx_contract_company_project", columnList = "com_id, project_id, contract_id")
        ,
        @Index(name = "idx_project_contract_company", columnList = "com_id, contract_id"),
        @Index(name = "idx_project_contract_company_store", columnList = "com_id, store_id, contract_id"),
        @Index(name = "idx_project_contract_company_team", columnList = "com_id, team_id, contract_id"),
        @Index(name = "idx_project_contract_company_owner", columnList = "com_id, owner_id, contract_id")
})
public class ProjectContract extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("合同索引")
    private Long contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

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


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @Note("唯一项目")
    private Project project;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    @Note("合同报价基线")
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Note("合同客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Column(length = 64, nullable = false)
    @Note("合同编号")
    private String no;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("原始合同金额")
    private BigDecimal baselineAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2)
    @Note("最终签约价格（签约时冻结，不随合同变更改写）")
    private BigDecimal signedAmount;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("当前合同金额")
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("合同状态")
    private ContractStatus status = ContractStatus.PendingSign;

    @Note("签约日期")
    private LocalDate signedAt;

    @Note("计划开工日期")
    private LocalDate startDate;

    @Note("计划完工日期")
    private LocalDate planEndDate;

    @Note("合同生效时间")
    private java.time.LocalDateTime effectiveAt;

    @Column(length = 128)
    @Note("付款条件")
    private String paymentTerms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @Note("创建人")
    private Employee createdBy;

    @Column(length = 500)
    @Note("合同说明")
    private String remark;

    @Transient
    @Note("应收计划数量")
    private Integer receivableCount = 0;

    @Transient
    @Unit("元")
    @Note("应收计划金额")
    private BigDecimal receivableAmount = BigDecimal.ZERO;

    @Transient
    @Unit("元")
    @Note("已收金额")
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Transient
    @Note("待审批变更数量")
    private Integer pendingChangeCount = 0;

    @Transient
    @Unit("元")
    @Note("签约成本快照")
    private BigDecimal signedCost;

    @Transient
    @Note("签约成本是否完整")
    private Boolean costComplete;

    @Transient
    @Note("付款计划快照")
    private ContractPaymentPlan paymentPlan;

    @Transient
    @Note("施工计划快照")
    private ContractConstructionPlan constructionPlan;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public BigDecimal getBaselineAmount() { return baselineAmount; }
    public void setBaselineAmount(BigDecimal baselineAmount) { this.baselineAmount = baselineAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDate signedAt) { this.signedAt = signedAt; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getReceivableCount() { return receivableCount; }
    public void setReceivableCount(Integer receivableCount) { this.receivableCount = receivableCount; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public Integer getPendingChangeCount() { return pendingChangeCount; }
    public void setPendingChangeCount(Integer pendingChangeCount) { this.pendingChangeCount = pendingChangeCount; }
    public BigDecimal getSignedCost() { return signedCost; }
    public void setSignedCost(BigDecimal signedCost) { this.signedCost = signedCost; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public ContractPaymentPlan getPaymentPlan() { return paymentPlan; }
    public void setPaymentPlan(ContractPaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }
    public ContractConstructionPlan getConstructionPlan() { return constructionPlan; }
    public void setConstructionPlan(ContractConstructionPlan constructionPlan) { this.constructionPlan = constructionPlan; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getPlanEndDate() { return planEndDate; }
    public void setPlanEndDate(LocalDate planEndDate) { this.planEndDate = planEndDate; }
    public java.time.LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(java.time.LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
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
