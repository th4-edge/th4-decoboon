package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 企业月度经营目标。 */
@Note("企业月度经营目标")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_business_target",
        indexes = {
                @Index(name = "idx_business_target_company", columnList = "com_id, target_id"),
                @Index(name = "idx_business_target_company_month", columnList = "com_id, target_month, target_id")
        })
public class BusinessTarget extends AuditModel {

    @Note("目标索引")
    @Id
    @SnowflakeId
    private Long targetId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("门店目标归属；NULL=公司级目标（DEC-076/BR-049）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("目标月份，保存当月第一天")
    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Note("签约目标")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal signedAmount = BigDecimal.ZERO;

    @Note("回款目标")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Note("毛利目标")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    @Note("在建项目目标数")
    @Column(nullable = false)
    private Integer activeProjectCount = 0;

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public LocalDate getTargetMonth() { return targetMonth; }
    public void setTargetMonth(LocalDate targetMonth) { this.targetMonth = targetMonth; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public Integer getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(Integer activeProjectCount) { this.activeProjectCount = activeProjectCount; }
}
