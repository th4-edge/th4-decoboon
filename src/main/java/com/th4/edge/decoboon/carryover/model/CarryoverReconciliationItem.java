package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceType;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationTheme;
import com.th4.edge.decoboon.common.model.AuditModel;
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

/** 双轨对账单主题：原账金额、装得清金额、差异与处理。 */
@Note("双轨对账主题")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "carryover_reconciliation_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_carryover_recon_theme",
                columnNames = {"reconciliation_id", "theme"}),
        indexes = {
                @Index(name = "idx_carryover_recon_item_company", columnList = "com_id, reconciliation_id, theme"),
                @Index(name = "idx_carryover_recon_item_owner", columnList = "com_id, owner_id, item_id")
        })
public class CarryoverReconciliationItem extends AuditModel {

    @Id
    @SnowflakeId
    @Note("主题行索引")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    @Note("所属对账批次")
    private CarryoverReconciliation reconciliation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Note("对账主题")
    private CarryoverReconciliationTheme theme;

    @Unit("元")
    @Column(name = "original_amount", precision = 14, scale = 2)
    @Note("原账金额")
    private BigDecimal originalAmount;

    @Column(name = "original_source", length = 500)
    @Note("原账来源说明")
    private String originalSource;

    @Column(name = "original_confirmed", nullable = false)
    @Note("原账金额是否已确认")
    private Boolean originalConfirmed = Boolean.FALSE;

    @Unit("元")
    @Column(name = "system_amount", precision = 14, scale = 2, nullable = false)
    @Note("装得清金额")
    private BigDecimal systemAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2)
    @Note("差异 = 原账 - 装得清")
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "difference_type", length = 20)
    @Note("差异分类")
    private CarryoverDifferenceType differenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difference_status", length = 20, nullable = false)
    @Note("差异状态")
    private CarryoverDifferenceStatus differenceStatus = CarryoverDifferenceStatus.Pending;

    @Column(length = 500)
    @Note("差异原因")
    private String reason;

    @Column(length = 500)
    @Note("处理动作")
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("责任人")
    private Employee owner;

    @Column(name = "due_date")
    @Note("处理时限")
    private LocalDate dueDate;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverReconciliation getReconciliation() { return reconciliation; }
    public void setReconciliation(CarryoverReconciliation reconciliation) { this.reconciliation = reconciliation; }
    public CarryoverReconciliationTheme getTheme() { return theme; }
    public void setTheme(CarryoverReconciliationTheme theme) { this.theme = theme; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public String getOriginalSource() { return originalSource; }
    public void setOriginalSource(String originalSource) { this.originalSource = originalSource; }
    public Boolean getOriginalConfirmed() { return originalConfirmed; }
    public void setOriginalConfirmed(Boolean originalConfirmed) { this.originalConfirmed = originalConfirmed; }
    public BigDecimal getSystemAmount() { return systemAmount; }
    public void setSystemAmount(BigDecimal systemAmount) { this.systemAmount = systemAmount; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public CarryoverDifferenceType getDifferenceType() { return differenceType; }
    public void setDifferenceType(CarryoverDifferenceType differenceType) { this.differenceType = differenceType; }
    public CarryoverDifferenceStatus getDifferenceStatus() { return differenceStatus; }
    public void setDifferenceStatus(CarryoverDifferenceStatus differenceStatus) { this.differenceStatus = differenceStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
