package com.th4.edge.decoboon.ai.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.ai.billing.configure.AiLedgerType;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Note("AI点数账本")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_ai_resource_ledger", indexes = {
        @Index(name = "idx_ai_ledger_company_time", columnList = "com_id, occurred_at, ledger_id"),
        @Index(name = "idx_ai_ledger_company_call", columnList = "com_id, call_id, ledger_id"),
        @Index(name = "idx_ai_ledger_company_store", columnList = "com_id, allocation_store_id, occurred_at")
})
public class AiResourceLedger extends AuditModel {

    @Id @SnowflakeId @Note("账本索引")
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "allocation_store_id")
    private Store allocationStore;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "call_id")
    private AiUsageCall call;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false)
    private AiLedgerType type;

    @Column(name = "change_units", nullable = false)
    private Long changeUnits;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "rate_card_version", length = 64)
    private String rateCardVersion;

    @Column(length = 300)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id")
    private Employee operator;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getAllocationStore() { return allocationStore; }
    public void setAllocationStore(Store allocationStore) { this.allocationStore = allocationStore; }
    public AiUsageCall getCall() { return call; }
    public void setCall(AiUsageCall call) { this.call = call; }
    public AiLedgerType getType() { return type; }
    public void setType(AiLedgerType type) { this.type = type; }
    public Long getChangeUnits() { return changeUnits; }
    public void setChangeUnits(Long changeUnits) { this.changeUnits = changeUnits; }
    public Long getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Long balanceBefore) { this.balanceBefore = balanceBefore; }
    public Long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Long balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getRateCardVersion() { return rateCardVersion; }
    public void setRateCardVersion(String rateCardVersion) { this.rateCardVersion = rateCardVersion; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
