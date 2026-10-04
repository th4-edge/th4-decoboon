package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.LedgerRevisionType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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
import jakarta.persistence.Table;

/** 手工流水更正审计：保留更正前后的完整快照。 */
@Note("手工流水更正审计")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_ledger_revision", indexes = {
        @Index(name = "idx_fin_revision_company", columnList = "com_id, revision_id"),
        @Index(name = "idx_fin_revision_ledger", columnList = "com_id, ledger_id, revision_id")
})
public class LedgerRevision extends AuditModel {

    @Id
    @SnowflakeId
    @Note("更正记录索引")
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    @Note("原流水")
    private Ledger ledger;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Note("更正类型")
    private LedgerRevisionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operated_by_id", nullable = false)
    @Note("操作人")
    private Employee operatedBy;

    @Column(length = 500, nullable = false)
    @Note("更正原因")
    private String reason;

    @Column(columnDefinition = "longtext", nullable = false)
    @Note("更正前快照")
    private String beforeSnapshot;

    @Column(columnDefinition = "longtext", nullable = false)
    @Note("更正后快照")
    private String afterSnapshot;

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Ledger getLedger() { return ledger; }
    public void setLedger(Ledger ledger) { this.ledger = ledger; }
    public LedgerRevisionType getType() { return type; }
    public void setType(LedgerRevisionType type) { this.type = type; }
    public Employee getOperatedBy() { return operatedBy; }
    public void setOperatedBy(Employee operatedBy) { this.operatedBy = operatedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterSnapshot() { return afterSnapshot; }
    public void setAfterSnapshot(String afterSnapshot) { this.afterSnapshot = afterSnapshot; }
}
