package com.th4.edge.decoboon.payroll.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemAttribution;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
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
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Note("提成回款释放记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_commission_release", indexes = {
        @Index(name = "idx_payroll_release_company", columnList = "com_id, release_id"),
        @Index(name = "idx_payroll_release_commission", columnList = "com_id, commission_id, release_id"),
        @Index(name = "idx_payroll_release_employee", columnList = "com_id, emp_id, release_id"),
        @Index(name = "idx_payroll_release_receipt", columnList = "com_id, receipt_id, release_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_release_commission_receipt",
                columnNames = {"com_id", "commission_id", "receipt_id"})
})
public class CommissionRelease extends AuditModel {

    @Id
    @SnowflakeId
    @Note("释放记录索引")
    private Long releaseId;

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
    @JoinColumn(name = "commission_id", nullable = false)
    private CommissionItem commissionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @Note("本次释放金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("释放日期")
    @Column(nullable = false)
    private LocalDate releasedAt;

    @Column(length = 500)
    private String remark;

    @Note("是否因收款冲正反释放")
    @Column(nullable = false)
    private Boolean reversed = Boolean.FALSE;

    @Note("反释放时间")
    private LocalDateTime reversedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private Employee reversedBy;

    @Column(name = "reverse_reason", length = 500)
    private String reverseReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getReleaseId() { return releaseId; }
    public void setReleaseId(Long releaseId) { this.releaseId = releaseId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public CommissionItem getCommissionItem() { return commissionItem; }
    public void setCommissionItem(CommissionItem commissionItem) { this.commissionItem = commissionItem; }
    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDate releasedAt) { this.releasedAt = releasedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public Boolean getReversed() { return reversed; }
    public void setReversed(Boolean reversed) { this.reversed = reversed; }
    public LocalDateTime getReversedAt() { return reversedAt; }
    public void setReversedAt(LocalDateTime reversedAt) { this.reversedAt = reversedAt; }
    public Employee getReversedBy() { return reversedBy; }
    public void setReversedBy(Employee reversedBy) { this.reversedBy = reversedBy; }
    public String getReverseReason() { return reverseReason; }
    public void setReverseReason(String reverseReason) { this.reverseReason = reverseReason; }
}
