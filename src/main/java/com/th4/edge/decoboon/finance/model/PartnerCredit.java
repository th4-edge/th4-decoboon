package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商往来余额（退货冲减后无可冲抵应付时形成）。
 *
 * <p>DEC-MP-02 延伸规则：已付款部分的退货金额优先冲抵同供应商其他未结清应付，
 * 仍有余款时转入供应商往来余额，供后续采购/付款抵扣，不修改历史付款流水。</p>
 */
@Note("供应商往来余额")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_finance_partner_credit", indexes = {
        @Index(name = "idx_partner_credit_company", columnList = "com_id, partner_credit_id"),
        @Index(name = "idx_partner_credit_company_partner", columnList = "com_id, partner_id, partner_credit_id")
})
public class PartnerCredit extends AuditModel {

    @Id
    @SnowflakeId
    @Note("往来余额索引")
    private Long partnerCreditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    @Note("供应商")
    private Partner partner;

    @Column(precision = 14, scale = 2, nullable = false)
    @Note("往来余额（可抵扣金额）")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 64)
    @Note("来源单号（退货单号）")
    private String sourceNo;

    @Note("生效日期")
    private LocalDate effectiveDate;

    @Column(length = 500)
    @Note("说明")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @Note("创建人")
    private Employee createdBy;

    public Long getPartnerCreditId() { return partnerCreditId; }
    public void setPartnerCreditId(Long partnerCreditId) { this.partnerCreditId = partnerCreditId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
