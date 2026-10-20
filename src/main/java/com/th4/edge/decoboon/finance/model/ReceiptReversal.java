package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceiptReversalReasonType;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 收款冲正事实。保留原收款与反向台账，不物理删除。 */
@Note("收款冲正")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_receipt_reversal",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fin_receipt_reversal_receipt", columnNames = "receipt_id"),
                @UniqueConstraint(name = "uk_fin_receipt_reversal_idempotency",
                        columnNames = {"com_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_fin_receipt_reversal_company", columnList = "com_id, reversal_id"),
                @Index(name = "idx_fin_receipt_reversal_project", columnList = "com_id, project_id, reversal_id"),
                @Index(name = "idx_fin_receipt_reversal_store", columnList = "com_id, store_id, reversal_id"),
                @Index(name = "idx_fin_receipt_reversal_date", columnList = "com_id, reversal_at, reversal_id")
        })
public class ReceiptReversal extends AuditModel {

    @Id
    @SnowflakeId
    @Note("冲正索引")
    private Long reversalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate reversalAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private ReceiptReversalReasonType reasonType;

    @Column(length = 500, nullable = false)
    private String reason;

    @Column(name = "idempotency_key", length = 128, nullable = false)
    private String idempotencyKey;

    @Column(name = "idempotency_fingerprint", length = 64, nullable = false)
    private String idempotencyFingerprint;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_ledger_id")
    private Ledger reversalLedger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_receipt_id")
    private Receipt replacementReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Employee operator;

    @Column(columnDefinition = "longtext", nullable = false)
    private String beforeSnapshot;

    @Column(columnDefinition = "longtext", nullable = false)
    private String afterSnapshot;

    public Long getReversalId() { return reversalId; }
    public void setReversalId(Long reversalId) { this.reversalId = reversalId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Receivable getReceivable() { return receivable; }
    public void setReceivable(Receivable receivable) { this.receivable = receivable; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getReversalAt() { return reversalAt; }
    public void setReversalAt(LocalDate reversalAt) { this.reversalAt = reversalAt; }
    public ReceiptReversalReasonType getReasonType() { return reasonType; }
    public void setReasonType(ReceiptReversalReasonType reasonType) { this.reasonType = reasonType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    public void setIdempotencyFingerprint(String idempotencyFingerprint) { this.idempotencyFingerprint = idempotencyFingerprint; }
    public Ledger getReversalLedger() { return reversalLedger; }
    public void setReversalLedger(Ledger reversalLedger) { this.reversalLedger = reversalLedger; }
    public Receipt getReplacementReceipt() { return replacementReceipt; }
    public void setReplacementReceipt(Receipt replacementReceipt) { this.replacementReceipt = replacementReceipt; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterSnapshot() { return afterSnapshot; }
    public void setAfterSnapshot(String afterSnapshot) { this.afterSnapshot = afterSnapshot; }
}
