package com.th4.edge.decoboon.risk.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.risk.configure.RiskAction;
import com.th4.edge.decoboon.risk.configure.RiskStatus;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险处置流水（append-only）。
 *
 * <p>参照 {@code AcceptanceRound} 与 {@code DEC-007} 的追加式留痕：写入后不可修改、不可删除，
 * 重开与关闭结论按时间线保留。</p>
 */
@Note("风险处置流水")
@Description("风险处置动作的追加式时间线：指派、认领、处置记录、催办、关闭、重开、自动消解")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_risk_disposition_log",
        indexes = {
                @Index(name = "idx_risk_log_company", columnList = "com_id, log_id"),
                @Index(name = "idx_risk_log_company_risk", columnList = "com_id, risk_id, log_id")
        })
public class RiskDispositionLog extends AuditModel {

    @Id
    @SnowflakeId
    @Note("处置流水索引")
    private Long logId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属处置单")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_id", nullable = false)
    private RiskItem risk;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20, nullable = false)
    @Note("处置动作")
    private RiskAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    @Note("迁移前状态")
    private RiskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    @Note("迁移后状态")
    private RiskStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    @Note("操作人")
    private Employee operator;

    @Note("处置说明")
    @Column(name = "note", length = 500)
    private String note;

    @ElementCollection
    @CollectionTable(name = "db_risk_log_evidence", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "evidence_url", length = 512)
    @Note("附件地址")
    private List<String> evidenceUrls;

    @Note("发生时间")
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public RiskItem getRisk() { return risk; }
    public void setRisk(RiskItem risk) { this.risk = risk; }
    public RiskAction getAction() { return action; }
    public void setAction(RiskAction action) { this.action = action; }
    public RiskStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(RiskStatus fromStatus) { this.fromStatus = fromStatus; }
    public RiskStatus getToStatus() { return toStatus; }
    public void setToStatus(RiskStatus toStatus) { this.toStatus = toStatus; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
