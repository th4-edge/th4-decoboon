package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 首月双轨对账批次。 */
@Note("首月双轨对账")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "carryover_reconciliation", indexes = {
        @Index(name = "idx_carryover_recon_company", columnList = "com_id, reconciliation_id"),
        @Index(name = "idx_carryover_recon_batch", columnList = "com_id, batch_id, reconciliation_id")
})
public class CarryoverReconciliation extends AuditModel {

    @Id
    @SnowflakeId
    @Note("对账索引")
    private Long reconciliationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @Note("所属承接任务")
    private CarryoverBatch batch;

    @Column(name = "as_of_date", nullable = false)
    @Note("接管日")
    private LocalDate asOfDate;

    @Column(name = "period_start", nullable = false)
    @Note("对账期间开始")
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    @Note("对账期间结束")
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Note("对账状态")
    private CarryoverReconciliationStatus status = CarryoverReconciliationStatus.Draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("对账负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Note("创建人")
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    @Note("确认人")
    private Employee confirmedBy;

    @Column(name = "confirmed_at")
    @Note("确认时间")
    private LocalDateTime confirmedAt;

    @Column(name = "closed_at")
    @Note("关闭时间")
    private LocalDateTime closedAt;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    public Long getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(Long reconciliationId) { this.reconciliationId = reconciliationId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverBatch getBatch() { return batch; }
    public void setBatch(CarryoverBatch batch) { this.batch = batch; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public CarryoverReconciliationStatus getStatus() { return status; }
    public void setStatus(CarryoverReconciliationStatus status) { this.status = status; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
