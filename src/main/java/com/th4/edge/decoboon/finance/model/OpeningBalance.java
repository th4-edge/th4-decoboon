package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 期初资金余额：接管日各资金账户的起始余额。
 *
 * <p>只作为账户起始余额，不计入收入、支出和利润；当前余额 = 期初余额 + 接管后净流量。</p>
 */
@Note("期初资金余额")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_opening_balance",
        uniqueConstraints = @UniqueConstraint(name = "uk_fin_opening_company_account_date",
                columnNames = {"com_id", "account", "as_of_date"}),
        indexes = {
                @Index(name = "idx_fin_opening_company_date", columnList = "com_id, as_of_date, opening_id")
        })
public class OpeningBalance extends AuditModel {

    @Id
    @SnowflakeId
    @Note("期初余额索引")
    private Long openingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("资金账户")
    private FundAccount account;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("接管日余额；允许负数表示透支")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "as_of_date", nullable = false)
    @Note("接管日")
    private LocalDate asOfDate;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @Note("创建人")
    private Employee createdBy;

    public Long getOpeningId() { return openingId; }
    public void setOpeningId(Long openingId) { this.openingId = openingId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public FundAccount getAccount() { return account; }
    public void setAccount(FundAccount account) { this.account = account; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
