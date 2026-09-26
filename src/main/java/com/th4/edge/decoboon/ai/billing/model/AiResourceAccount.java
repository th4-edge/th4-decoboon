package com.th4.edge.decoboon.ai.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.ai.billing.configure.AiAccountStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

@Note("AI资源账户")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_ai_resource_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_resource_account_company", columnNames = "com_id"),
        indexes = @Index(name = "idx_ai_resource_account_company", columnList = "com_id, account_id"))
public class AiResourceAccount extends AuditModel {

    @Id @SnowflakeId @Note("账户索引")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Unit("点") @Column(name = "total_granted", nullable = false)
    private Long totalGranted = 0L;
    @Unit("点") @Column(name = "available_units", nullable = false)
    private Long availableUnits = 0L;
    @Unit("点") @Column(name = "reserved_units", nullable = false)
    private Long reservedUnits = 0L;
    @Unit("点") @Column(name = "consumed_units", nullable = false)
    private Long consumedUnits = 0L;
    @Unit("点") @Column(name = "refunded_units", nullable = false)
    private Long refundedUnits = 0L;
    @Unit("点") @Column(name = "adjusted_units", nullable = false)
    private Long adjustedUnits = 0L;

    @Column(name = "balance_version", nullable = false)
    @Note("余额版本，每次变动递增")
    private Long balanceVersion = 0L;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false)
    @Note("账户状态")
    private AiAccountStatus status = AiAccountStatus.Active;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Long getTotalGranted() { return totalGranted; }
    public void setTotalGranted(Long totalGranted) { this.totalGranted = totalGranted; }
    public Long getAvailableUnits() { return availableUnits; }
    public void setAvailableUnits(Long availableUnits) { this.availableUnits = availableUnits; }
    public Long getReservedUnits() { return reservedUnits; }
    public void setReservedUnits(Long reservedUnits) { this.reservedUnits = reservedUnits; }
    public Long getConsumedUnits() { return consumedUnits; }
    public void setConsumedUnits(Long consumedUnits) { this.consumedUnits = consumedUnits; }
    public Long getRefundedUnits() { return refundedUnits; }
    public void setRefundedUnits(Long refundedUnits) { this.refundedUnits = refundedUnits; }
    public Long getAdjustedUnits() { return adjustedUnits; }
    public void setAdjustedUnits(Long adjustedUnits) { this.adjustedUnits = adjustedUnits; }
    public Long getBalanceVersion() { return balanceVersion; }
    public void setBalanceVersion(Long balanceVersion) { this.balanceVersion = balanceVersion; }
    public AiAccountStatus getStatus() { return status; }
    public void setStatus(AiAccountStatus status) { this.status = status; }
}
