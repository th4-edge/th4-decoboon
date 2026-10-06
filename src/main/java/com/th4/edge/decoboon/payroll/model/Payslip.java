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

@Note("工资条")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_payslip", indexes = {
        @Index(name = "idx_payroll_payslip_company", columnList = "com_id, payslip_id"),
        @Index(name = "idx_payroll_payslip_sheet", columnList = "com_id, sheet_id, payslip_id"),
        @Index(name = "idx_payroll_payslip_employee", columnList = "com_id, emp_id, period, payslip_id"),
        @Index(name = "idx_payroll_payslip_published", columnList = "com_id, emp_id, published_at, payslip_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_payslip_sheet_employee",
                columnNames = {"com_id", "sheet_id", "emp_id"})
})
public class Payslip extends AuditModel {

    @Id
    @SnowflakeId
    @Note("工资条索引")
    private Long payslipId;

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
    @JoinColumn(name = "sheet_id", nullable = false)
    private PayrollSheet sheet;

    @Column(length = 7, nullable = false)
    private String period;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private PayslipStatus status = PayslipStatus.PendingPayment;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getPayslipId() { return payslipId; }
    public void setPayslipId(Long payslipId) { this.payslipId = payslipId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public PayrollSheet getSheet() { return sheet; }
    public void setSheet(PayrollSheet sheet) { this.sheet = sheet; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public PayslipStatus getStatus() { return status; }
    public void setStatus(PayslipStatus status) { this.status = status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
