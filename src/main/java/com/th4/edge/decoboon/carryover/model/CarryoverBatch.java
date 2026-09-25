package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverBatchStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
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

/** 在跑项目承接任务。 */
@Note("在跑项目承接任务")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_batch", indexes = {
        @Index(name = "idx_carryover_batch_company", columnList = "com_id, batch_id"),
        @Index(name = "idx_carryover_batch_company_status", columnList = "com_id, status, batch_id")
})
public class CarryoverBatch extends AuditModel {

    @Note("承接任务索引")
    @Id
    @SnowflakeId
    private Long batchId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("发起人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;

    @Note("接管日期")
    @Column(nullable = false)
    private LocalDate asOfDate;

    @Note("任务状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CarryoverBatchStatus status = CarryoverBatchStatus.Active;

    @Note("范围确认时间")
    private LocalDateTime scopeConfirmedAt;

    @Note("完成时间")
    private LocalDateTime completedAt;

    @Note("任务版本")
    @Column(nullable = false)
    private Integer version = 1;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public CarryoverBatchStatus getStatus() { return status; }
    public void setStatus(CarryoverBatchStatus status) { this.status = status; }
    public LocalDateTime getScopeConfirmedAt() { return scopeConfirmedAt; }
    public void setScopeConfirmedAt(LocalDateTime scopeConfirmedAt) { this.scopeConfirmedAt = scopeConfirmedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
