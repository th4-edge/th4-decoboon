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

    @Column(nullable = false)
    @Note("正常出勤天数")
    private Integer normalDays = 0;

    @Column(nullable = false)
    @Note("事假天数")
    private Integer personalLeaveDays = 0;

    @Column(nullable = false)
    @Note("病假天数")
    private Integer sickLeaveDays = 0;

    @Column(nullable = false)
    @Note("旷工天数")
    private Integer absenceDays = 0;

    @Column(nullable = false)
    @Note("外勤天数")
    private Integer externalDays = 0;

    @Column(nullable = false)
    @Note("年假天数")
    private Integer annualLeaveDays = 0;

    @Column(nullable = false)
    @Note("调休天数")
    private Integer compLeaveDays = 0;

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
    public Integer getNormalDays() { return normalDays; }
    public void setNormalDays(Integer normalDays) { this.normalDays = normalDays; }
    public Integer getPersonalLeaveDays() { return personalLeaveDays; }
    public void setPersonalLeaveDays(Integer personalLeaveDays) { this.personalLeaveDays = personalLeaveDays; }
    public Integer getSickLeaveDays() { return sickLeaveDays; }
    public void setSickLeaveDays(Integer sickLeaveDays) { this.sickLeaveDays = sickLeaveDays; }
    public Integer getAbsenceDays() { return absenceDays; }
    public void setAbsenceDays(Integer absenceDays) { this.absenceDays = absenceDays; }
    public Integer getExternalDays() { return externalDays; }
    public void setExternalDays(Integer externalDays) { this.externalDays = externalDays; }
    public Integer getAnnualLeaveDays() { return annualLeaveDays; }
    public void setAnnualLeaveDays(Integer annualLeaveDays) { this.annualLeaveDays = annualLeaveDays; }
    public Integer getCompLeaveDays() { return compLeaveDays; }
    public void setCompLeaveDays(Integer compLeaveDays) { this.compLeaveDays = compLeaveDays; }
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
