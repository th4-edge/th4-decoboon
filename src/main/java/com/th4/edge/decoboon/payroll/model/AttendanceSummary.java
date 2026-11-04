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

@Note("月度考勤汇总")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_attendance_summary", indexes = {
        @Index(name = "idx_payroll_attendance_company", columnList = "com_id, summary_id"),
        @Index(name = "idx_payroll_attendance_employee", columnList = "com_id, emp_id, payroll_month, summary_id"),
        @Index(name = "idx_payroll_attendance_store", columnList = "com_id, store_id, payroll_month, summary_id"),
        @Index(name = "idx_payroll_attendance_team", columnList = "com_id, team_id, payroll_month, summary_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_attendance_employee_month",
                columnNames = {"com_id", "emp_id", "payroll_month"})
})
public class AttendanceSummary extends AuditModel {

    @Id
    @SnowflakeId
    @Note("考勤汇总索引")
    private Long summaryId;

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

    @Column(name = "payroll_month", length = 7, nullable = false)
    @Note("考勤月份")
    private String payrollMonth;

    @Column(nullable = false)
    @Note("应出勤天数")
    private Integer scheduledDays = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("正常出勤天数")
    private BigDecimal normalDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("事假天数")
    private BigDecimal personalLeaveDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("病假天数")
    private BigDecimal sickLeaveDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("旷工天数")
    private BigDecimal absenceDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("外勤天数")
    private BigDecimal externalDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("年假天数")
    private BigDecimal annualLeaveDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Note("调休天数")
    private BigDecimal compLeaveDays = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("考勤状态")
    private AttendanceStatus status = AttendanceStatus.Draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Employee confirmedBy;

    private LocalDateTime confirmedAt;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getSummaryId() { return summaryId; }
    public void setSummaryId(Long summaryId) { this.summaryId = summaryId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(String payrollMonth) { this.payrollMonth = payrollMonth; }
    public Integer getScheduledDays() { return scheduledDays; }
    public void setScheduledDays(Integer scheduledDays) { this.scheduledDays = scheduledDays; }
    public BigDecimal getNormalDays() { return normalDays; }
    public void setNormalDays(BigDecimal normalDays) { this.normalDays = normalDays; }
    public BigDecimal getPersonalLeaveDays() { return personalLeaveDays; }
    public void setPersonalLeaveDays(BigDecimal personalLeaveDays) { this.personalLeaveDays = personalLeaveDays; }
    public BigDecimal getSickLeaveDays() { return sickLeaveDays; }
    public void setSickLeaveDays(BigDecimal sickLeaveDays) { this.sickLeaveDays = sickLeaveDays; }
    public BigDecimal getAbsenceDays() { return absenceDays; }
    public void setAbsenceDays(BigDecimal absenceDays) { this.absenceDays = absenceDays; }
    public BigDecimal getExternalDays() { return externalDays; }
    public void setExternalDays(BigDecimal externalDays) { this.externalDays = externalDays; }
    public BigDecimal getAnnualLeaveDays() { return annualLeaveDays; }
    public void setAnnualLeaveDays(BigDecimal annualLeaveDays) { this.annualLeaveDays = annualLeaveDays; }
    public BigDecimal getCompLeaveDays() { return compLeaveDays; }
    public void setCompLeaveDays(BigDecimal compLeaveDays) { this.compLeaveDays = compLeaveDays; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
