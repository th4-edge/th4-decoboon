package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 应付款核销明细。
 *
 * <p>一笔付款可冲抵同一供应商的多张应付单，付款与应付因而是多对多关系；
 * 本表记录每次核销的金额来源与去向。名称刻意区别于项目结算 {@code Settlement}。</p>
 */
@Note("应付款核销明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_finance_payable_settlement", indexes = {
        @Index(name = "idx_settle_company", columnList = "com_id, settlement_id"),
        @Index(name = "idx_settle_company_payment", columnList = "com_id, payment_id, settlement_id"),
        @Index(name = "idx_settle_company_payable", columnList = "com_id, payable_id, settlement_id"),
        @Index(name = "idx_settle_company_store", columnList = "com_id, store_id, settlement_id"),
        @Index(name = "idx_settle_company_team", columnList = "com_id, team_id, settlement_id"),
        @Index(name = "idx_settle_company_owner", columnList = "com_id, owner_id, settlement_id")
})
public class PayableSettlement extends AuditModel {

    @Note("核销索引")
    @Id
    @SnowflakeId
    private Long settlementId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @Note("付款单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Note("应付单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;

    @Note("核销金额")
    @Unit("分")
    @Column(nullable = false)
    private Long amount = 0L;

    @Note("备注")
    @Column(length = 255)
    private String remark;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Payable getPayable() { return payable; }
    public void setPayable(Payable payable) { this.payable = payable; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
