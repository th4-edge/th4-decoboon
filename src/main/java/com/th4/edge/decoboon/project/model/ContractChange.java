package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
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
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 合同金额变更单。 */
@Note("合同变更单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_contract_change", indexes = {
        @Index(name = "idx_change_company", columnList = "com_id, change_id"),
        @Index(name = "idx_change_company_contract", columnList = "com_id, contract_id, change_id"),
        @Index(name = "idx_change_company_project", columnList = "com_id, project_id, change_id"),
        @Index(name = "idx_contract_change_company_project_status", columnList = "com_id, project_id, status, change_id")
,
        @Index(name = "idx_contract_change_company", columnList = "com_id, change_id"),
        @Index(name = "idx_contract_change_company_store", columnList = "com_id, store_id, change_id"),
        @Index(name = "idx_contract_change_company_team", columnList = "com_id, team_id, change_id"),
        @Index(name = "idx_contract_change_company_owner", columnList = "com_id, owner_id, change_id")
})
public class ContractChange extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("变更索引")
    private Long changeId;

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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @Note("合同基线")
    private ProjectContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Column(length = 64, nullable = false, unique = true)
    @Note("变更单号")
    private String no;

    @Column(length = 500, nullable = false)
    @Note("变更原因")
    private String reason;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("变更金额，正数增加、负数减少")
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("变更状态")
    private ContractChangeStatus status = ContractChangeStatus.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @Note("发起人")
    private Employee requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Note("变更类型")
    private ContractChangeType type = ContractChangeType.Add;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_line_id")
    @Note("原合同行")
    private ContractLine sourceLine;

    @Unit("元")
    @Column(precision = 14, scale = 2)
    @Note("成本变化；null 表示存在成本未完整")
    private BigDecimal costDelta;

    @Column(nullable = false)
    @Note("客户是否确认")
    private Boolean customerConfirmed = Boolean.FALSE;

    @Note("客户确认时间")
    private LocalDateTime customerConfirmedAt;

    @Column(length = 64)
    @Note("客户确认人")
    private String customerConfirmedBy;

    @Column(length = 32)
    @Note("客户确认方式")
    private String customerConfirmMethod;

    @ElementCollection
    @CollectionTable(name = "db_contract_change_image", joinColumns = @JoinColumn(name = "change_id"))
    @Column(name = "url", length = 512)
    @Note("签证照片")
    private List<String> evidenceImages;

    @Column(length = 512)
    @Note("客户签认单")
    private String signatureUrl;

    @Transient
    @Note("变更报价明细")
    private List<ContractChangeItem> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    @Note("审批人")
    private Employee approvedBy;

    @Column(length = 500)
    @Note("审批意见")
    private String conclusion;

    @Note("审批时间")
    private LocalDateTime approvedAt;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public ContractChangeStatus getStatus() { return status; }
    public void setStatus(ContractChangeStatus status) { this.status = status; }
    public Employee getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Employee requestedBy) { this.requestedBy = requestedBy; }
    public Employee getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Employee approvedBy) { this.approvedBy = approvedBy; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public ContractChangeType getType() { return type; }
    public void setType(ContractChangeType type) { this.type = type; }
    public ContractLine getSourceLine() { return sourceLine; }
    public void setSourceLine(ContractLine sourceLine) { this.sourceLine = sourceLine; }
    public BigDecimal getCostDelta() { return costDelta; }
    public void setCostDelta(BigDecimal costDelta) { this.costDelta = costDelta; }
    public Boolean getCustomerConfirmed() { return customerConfirmed; }
    public void setCustomerConfirmed(Boolean customerConfirmed) { this.customerConfirmed = customerConfirmed; }
    public LocalDateTime getCustomerConfirmedAt() { return customerConfirmedAt; }
    public void setCustomerConfirmedAt(LocalDateTime customerConfirmedAt) { this.customerConfirmedAt = customerConfirmedAt; }
    public String getCustomerConfirmedBy() { return customerConfirmedBy; }
    public void setCustomerConfirmedBy(String customerConfirmedBy) { this.customerConfirmedBy = customerConfirmedBy; }
    public String getCustomerConfirmMethod() { return customerConfirmMethod; }
    public void setCustomerConfirmMethod(String customerConfirmMethod) { this.customerConfirmMethod = customerConfirmMethod; }
    public List<String> getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(List<String> evidenceImages) { this.evidenceImages = evidenceImages; }
    public String getSignatureUrl() { return signatureUrl; }
    public void setSignatureUrl(String signatureUrl) { this.signatureUrl = signatureUrl; }
    public List<ContractChangeItem> getItems() { return items; }
    public void setItems(List<ContractChangeItem> items) { this.items = items; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
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
