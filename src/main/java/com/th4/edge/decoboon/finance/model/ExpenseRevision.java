package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** 费用首次提交和重新提交的不可变业务快照。 */
@Note("费用提交版本")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_finance_expense_revision",
        uniqueConstraints = @UniqueConstraint(name = "uk_fin_expense_revision_company_expense_version",
                columnNames = {"com_id", "expense_id", "version"}),
        indexes = {
                @Index(name = "idx_fin_expense_revision_company", columnList = "com_id, revision_id"),
                @Index(name = "idx_fin_expense_revision_expense", columnList = "com_id, expense_id, version")
        })
public class ExpenseRevision extends AuditModel {

    @Id
    @SnowflakeId
    @Note("版本索引")
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    @Note("所属费用")
    private Expense expense;

    @Column(nullable = false)
    @Note("提交版本")
    private Integer version;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "longtext")
    @Note("业务快照")
    private String snapshotJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    @Note("提交人")
    private Employee submittedBy;

    @Column(name = "submitted_at", nullable = false)
    @Note("提交时间")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by_id")
    @Note("上一次退回人")
    private Employee returnedBy;

    @Column(name = "returned_at")
    @Note("上一次退回时间")
    private LocalDateTime returnedAt;

    @Lob
    @Column(name = "return_reason", columnDefinition = "longtext")
    @Note("上一次退回原因")
    private String returnReason;

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public Employee getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Employee submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Employee getReturnedBy() { return returnedBy; }
    public void setReturnedBy(Employee returnedBy) { this.returnedBy = returnedBy; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
}
