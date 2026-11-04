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

@Note("工资单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_payroll_sheet", indexes = {
        @Index(name = "idx_payroll_sheet_company", columnList = "com_id, sheet_id"),
        @Index(name = "idx_payroll_sheet_period", columnList = "com_id, period, sheet_id"),
        @Index(name = "idx_payroll_sheet_owner", columnList = "com_id, owner_id, sheet_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_sheet_company_period", columnNames = {"com_id", "period"})
})
public class PayrollSheet extends AuditModel {

    @Id
    @SnowflakeId
    @Note("工资单索引")
    private Long sheetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @Column(length = 7, nullable = false)
    @Note("工资周期")
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("工资单状态")
    private PayrollSheetStatus status = PayrollSheetStatus.Draft;

    @Column(nullable = false)
    @Note("考勤是否完整")
    private Boolean attendanceComplete = Boolean.FALSE;

    @Column(nullable = false)
    @Note("提成是否完整")
    private Boolean commissionComplete = Boolean.FALSE;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal totalGrossAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal totalDeductionAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal totalNetAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Employee confirmedBy;

    private LocalDateTime confirmedAt;

    @Column(length = 500)
    private String remark;

    /** 门店范围返回的只读标记：工资单内存在其他门店工资项。 */
    @Transient
    private Boolean mixedStore = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public PayrollSheetStatus getStatus() { return status; }
    public void setStatus(PayrollSheetStatus status) { this.status = status; }
    public Boolean getAttendanceComplete() { return attendanceComplete; }
    public void setAttendanceComplete(Boolean attendanceComplete) { this.attendanceComplete = attendanceComplete; }
    public Boolean getCommissionComplete() { return commissionComplete; }
    public void setCommissionComplete(Boolean commissionComplete) { this.commissionComplete = commissionComplete; }
    public BigDecimal getTotalGrossAmount() { return totalGrossAmount; }
    public void setTotalGrossAmount(BigDecimal totalGrossAmount) { this.totalGrossAmount = totalGrossAmount; }
    public BigDecimal getTotalDeductionAmount() { return totalDeductionAmount; }
    public void setTotalDeductionAmount(BigDecimal totalDeductionAmount) { this.totalDeductionAmount = totalDeductionAmount; }
    public BigDecimal getTotalNetAmount() { return totalNetAmount; }
    public void setTotalNetAmount(BigDecimal totalNetAmount) { this.totalNetAmount = totalNetAmount; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Boolean getMixedStore() { return mixedStore; }
    public void setMixedStore(Boolean mixedStore) { this.mixedStore = mixedStore; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
