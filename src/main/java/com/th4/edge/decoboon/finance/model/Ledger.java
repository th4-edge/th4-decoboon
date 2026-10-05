package com.th4.edge.decoboon.finance.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.project.model.Project;
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
import java.time.LocalDateTime;

/** 不可变资金流水，所有收付款与手工记账最后归集到此处。 */
@Note("资金流水")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_ledger", indexes = {
        @Index(name = "idx_fin_ledger_company", columnList = "com_id, ledger_id"),
        @Index(name = "idx_fin_ledger_project", columnList = "com_id, project_id, ledger_id"),
        @Index(name = "idx_fin_ledger_store", columnList = "com_id, store_id, ledger_id"),
        @Index(name = "idx_fin_ledger_team", columnList = "com_id, team_id, ledger_id"),
        @Index(name = "idx_fin_ledger_owner", columnList = "com_id, owner_id, ledger_id")
})
public class Ledger extends AuditModel {

    @Note("流水索引")
    @Id
    @SnowflakeId
    private Long ledgerId;

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

    @Note("收支方向")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerDirection direction;

    @Note("业务分类/手工分类说明")
    @Keyword
    @Column(length = 128, nullable = false)
    private String category;

    @Note("手工账务用途；自动流水为空")
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private LedgerManualPurpose purpose;

    @Note("结构化业务分类；历史文本分类为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private LedgerCategory categoryNode;

    @Note("被冲正或更正的原流水")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_ledger_id")
    private Ledger originalLedger;

    @Note("冲正重记分组标识")
    @Column(length = 64)
    private String correctionGroupId;

    @Note("是否作废；作废后保留审计，不再进入台账与汇总")
    @Column(nullable = false)
    private Boolean voided = Boolean.FALSE;

    @Note("作废人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_id")
    private Employee voidedBy;

    @Note("作废时间")
    private LocalDateTime voidedAt;

    @Note("作废原因")
    @Column(length = 500)
    private String voidReason;

    @Note("资金来源")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerSource source;

    @Note("来源单索引")
    private Long sourceId;

    @Note("金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("资金账户")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private FundAccount account = FundAccount.CompanyBank;

    @Note("发生日期")
    @Column(nullable = false)
    private LocalDate occurredAt;

    @Note("往来单位或个人")
    @Keyword
    @Column(length = 128)
    private String counterparty;

    @Note("摘要")
    @Column(length = 256, nullable = false)
    private String summary;

    @Note("备注")
    @Column(length = 500)
    private String remark;

    @Note("经办人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
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
    public LedgerDirection getDirection() { return direction; }
    public void setDirection(LedgerDirection direction) { this.direction = direction; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LedgerManualPurpose getPurpose() { return purpose; }
    public void setPurpose(LedgerManualPurpose purpose) { this.purpose = purpose; }
    public LedgerCategory getCategoryNode() { return categoryNode; }
    public void setCategoryNode(LedgerCategory categoryNode) { this.categoryNode = categoryNode; }
    public Ledger getOriginalLedger() { return originalLedger; }
    public void setOriginalLedger(Ledger originalLedger) { this.originalLedger = originalLedger; }
    public String getCorrectionGroupId() { return correctionGroupId; }
    public void setCorrectionGroupId(String correctionGroupId) { this.correctionGroupId = correctionGroupId; }
    public LedgerSource getSource() { return source; }
    public void setSource(LedgerSource source) { this.source = source; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public FundAccount getAccount() { return account; }
    public void setAccount(FundAccount account) { this.account = account; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDate occurredAt) { this.occurredAt = occurredAt; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public Boolean getVoided() { return voided; }
    public void setVoided(Boolean voided) { this.voided = voided; }
    public Employee getVoidedBy() { return voidedBy; }
    public void setVoidedBy(Employee voidedBy) { this.voidedBy = voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
}
