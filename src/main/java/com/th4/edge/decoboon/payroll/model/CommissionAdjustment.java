package com.th4.edge.decoboon.payroll.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentType;
import com.th4.edge.decoboon.project.model.Project;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Note("员工提成负向调整")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_commission_adjustment", indexes = {
        @Index(name = "idx_payroll_adjustment_company", columnList = "com_id, adjustment_id"),
        @Index(name = "idx_payroll_adjustment_employee", columnList = "com_id, emp_id, status, adjustment_id"),
        @Index(name = "idx_payroll_adjustment_original", columnList = "com_id, original_commission_id, adjustment_id"),
        @Index(name = "idx_payroll_adjustment_project", columnList = "com_id, project_id, adjustment_id"),
        @Index(name = "idx_payroll_adjustment_payroll_item", columnList = "com_id, payroll_item_id, adjustment_id"),
        @Index(name = "idx_payroll_adjustment_store", columnList = "com_id, store_id, adjustment_id")
})
public class CommissionAdjustment extends AuditModel {

    @Id
    @SnowflakeId
    @Note("调整索引")
    private Long adjustmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_commission_id", nullable = false)
    private CommissionItem originalCommission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private ReceiptRefund refund;

    @Column(length = 128)
    private String idempotencyKey;

    @Column(length = 64)
    private String idempotencyFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_item_id")
    private PayrollItem payrollItem;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("调整类型")
    private CommissionAdjustmentType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("调整状态")
    private CommissionAdjustmentStatus status = CommissionAdjustmentStatus.Pending;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate occurredAt;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(Long adjustmentId) { this.adjustmentId = adjustmentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public CommissionItem getOriginalCommission() { return originalCommission; }
    public void setOriginalCommission(CommissionItem originalCommission) { this.originalCommission = originalCommission; }
    public ReceiptRefund getRefund() { return refund; }
    public void setRefund(ReceiptRefund refund) { this.refund = refund; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    public void setIdempotencyFingerprint(String idempotencyFingerprint) { this.idempotencyFingerprint = idempotencyFingerprint; }
    public PayrollItem getPayrollItem() { return payrollItem; }
    public void setPayrollItem(PayrollItem payrollItem) { this.payrollItem = payrollItem; }
    public CommissionAdjustmentType getType() { return type; }
    public void setType(CommissionAdjustmentType type) { this.type = type; }
    public CommissionAdjustmentStatus getStatus() { return status; }
    public void setStatus(CommissionAdjustmentStatus status) { this.status = status; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }
    public BigDecimal getAppliedAmount() { return appliedAmount; }
    public void setAppliedAmount(BigDecimal appliedAmount) { this.appliedAmount = appliedAmount; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDate occurredAt) { this.occurredAt = occurredAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
