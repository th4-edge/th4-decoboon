package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.FundAccount;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 客户退款/退单事实，按原收款追溯并参与有效回款净额计算。 */
@Note("客户退款")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_refund", indexes = {
        @Index(name = "idx_fin_refund_company", columnList = "com_id, refund_id"),
        @Index(name = "idx_fin_refund_project", columnList = "com_id, project_id, refund_id"),
        @Index(name = "idx_fin_refund_receipt", columnList = "com_id, receipt_id, refund_id"),
        @Index(name = "idx_fin_refund_customer", columnList = "com_id, customer_id, refund_id"),
        @Index(name = "idx_fin_refund_store", columnList = "com_id, store_id, refund_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_fin_refund_company_idempotency",
                columnNames = {"com_id", "idempotency_key"})
})
public class ReceiptRefund extends AuditModel {

    @Id
    @SnowflakeId
    @Note("退款索引")
    private Long refundId;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private FundAccount account = FundAccount.CompanyBank;

    @Column(nullable = false)
    private LocalDate refundAt;

    @Column(length = 500)
    private String reason;

    @Column(length = 128)
    private String idempotencyKey;

    @Column(length = 64)
    private String idempotencyFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
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
    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public FundAccount getAccount() { return account; }
    public void setAccount(FundAccount account) { this.account = account; }
    public LocalDate getRefundAt() { return refundAt; }
    public void setRefundAt(LocalDate refundAt) { this.refundAt = refundAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    public void setIdempotencyFingerprint(String idempotencyFingerprint) { this.idempotencyFingerprint = idempotencyFingerprint; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
