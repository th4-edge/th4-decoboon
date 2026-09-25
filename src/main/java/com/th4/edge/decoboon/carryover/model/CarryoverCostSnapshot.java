package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 接管前成本快照。 */
@Note("接管前成本快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_cost_snapshot", indexes = {
        @Index(name = "idx_carryover_cost_item", columnList = "com_id, item_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_carryover_cost_item", columnNames = "item_id"),
        @UniqueConstraint(name = "uk_carryover_cost_project", columnNames = "project_id")
})
public class CarryoverCostSnapshot extends AuditModel {

    @Note("成本快照索引")
    @Id
    @SnowflakeId
    private Long snapshotId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属承接项目")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private CarryoverProject item;

    @Note("正式项目")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("接管前成本总额")
    @Unit("元")
    @Column(name = "total_cost", precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Note("成本完整性")
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_status", length = 16, nullable = false)
    private CarryoverCostStatus costStatus = CarryoverCostStatus.Unknown;

    @Note("成本截止日")
    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Note("缺失说明")
    @Column(name = "missing_note", length = 500)
    private String missingNote;

    @Note("确认人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Employee confirmedBy;

    @Note("确认时间")
    private LocalDateTime confirmedAt;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public CarryoverCostStatus getCostStatus() { return costStatus; }
    public void setCostStatus(CarryoverCostStatus costStatus) { this.costStatus = costStatus; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public String getMissingNote() { return missingNote; }
    public void setMissingNote(String missingNote) { this.missingNote = missingNote; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
