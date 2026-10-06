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

@Note("工资项")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_item", indexes = {
        @Index(name = "idx_payroll_item_company", columnList = "com_id, item_id"),
        @Index(name = "idx_payroll_item_sheet", columnList = "com_id, sheet_id, item_id"),
        @Index(name = "idx_payroll_item_employee", columnList = "com_id, emp_id, sheet_id, item_id"),
        @Index(name = "idx_payroll_item_project", columnList = "com_id, project_id, item_id"),
        @Index(name = "idx_payroll_item_store", columnList = "com_id, store_id, item_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_item_sheet_employee_attribution",
                columnNames = {"com_id", "sheet_id", "emp_id", "attribution_key"})
})
public class PayrollItem extends AuditModel {

    @Id
    @SnowflakeId
    @Note("工资项索引")
    private Long itemId;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("归属类型")
    private PayrollItemAttribution attribution = PayrollItemAttribution.Company;

    @Column(name = "attribution_key", length = 64, nullable = false)
    @Note("归属键：PROJECT:项目ID 或 COMPANY")
    private String attributionKey;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal baseSalaryAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal attendanceDeduction = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal allowanceAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal socialInsuranceAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

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
    @Column(length = 16, nullable = false)
    @Note("工资项状态")
    private PayrollItemStatus status = PayrollItemStatus.Draft;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
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
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public PayrollItemAttribution getAttribution() { return attribution; }
    public void setAttribution(PayrollItemAttribution attribution) { this.attribution = attribution; }
    public String getAttributionKey() { return attributionKey; }
    public void setAttributionKey(String attributionKey) { this.attributionKey = attributionKey; }
    public BigDecimal getBaseSalaryAmount() { return baseSalaryAmount; }
    public void setBaseSalaryAmount(BigDecimal baseSalaryAmount) { this.baseSalaryAmount = baseSalaryAmount; }
    public BigDecimal getAttendanceDeduction() { return attendanceDeduction; }
    public void setAttendanceDeduction(BigDecimal attendanceDeduction) { this.attendanceDeduction = attendanceDeduction; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(BigDecimal bonusAmount) { this.bonusAmount = bonusAmount; }
    public BigDecimal getAllowanceAmount() { return allowanceAmount; }
    public void setAllowanceAmount(BigDecimal allowanceAmount) { this.allowanceAmount = allowanceAmount; }
    public BigDecimal getSocialInsuranceAmount() { return socialInsuranceAmount; }
    public void setSocialInsuranceAmount(BigDecimal socialInsuranceAmount) { this.socialInsuranceAmount = socialInsuranceAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getOtherDeduction() { return otherDeduction; }
    public void setOtherDeduction(BigDecimal otherDeduction) { this.otherDeduction = otherDeduction; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public PayrollItemStatus getStatus() { return status; }
    public void setStatus(PayrollItemStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
