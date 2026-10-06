package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 凭证修订轨迹：记录补录工人前后值与操作人，不承载金额或核销事实。 */
@Note("凭证修订轨迹")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_voucher_revision", indexes = {
        @Index(name = "idx_voucher_revision_company", columnList = "com_id, revision_id"),
        @Index(name = "idx_voucher_revision_voucher", columnList = "com_id, voucher_id, revision_id"),
        @Index(name = "idx_voucher_revision_operator", columnList = "com_id, operator_id, revision_id")
})
public class VoucherRevision extends AuditModel {

    @Id
    @SnowflakeId
    @Note("修订索引")
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    @Note("修订凭证")
    private VoucherRecord voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_worker_id")
    @Note("补录前工人")
    private Worker beforeWorker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_worker_id")
    @Note("补录后工人")
    private Worker afterWorker;

    @Column(name = "change_type", length = 32, nullable = false)
    @Note("修订类型")
    private String changeType = "WorkerBackfill";

    @Column(length = 500)
    @Note("修订说明")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    @Note("操作人")
    private Employee operator;

    @Column(name = "changed_at", nullable = false)
    @Note("变更时间")
    private LocalDateTime changedAt = LocalDateTime.now();

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public VoucherRecord getVoucher() { return voucher; }
    public void setVoucher(VoucherRecord voucher) { this.voucher = voucher; }
    public Worker getBeforeWorker() { return beforeWorker; }
    public void setBeforeWorker(Worker beforeWorker) { this.beforeWorker = beforeWorker; }
    public Worker getAfterWorker() { return afterWorker; }
    public void setAfterWorker(Worker afterWorker) { this.afterWorker = afterWorker; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
