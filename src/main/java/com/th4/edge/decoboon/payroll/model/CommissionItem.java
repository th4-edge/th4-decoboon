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

@Note("员工提成明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "employee")
@Table(name = "db_payroll_commission_item", indexes = {
        @Index(name = "idx_payroll_commission_company", columnList = "com_id, commission_id"),
        @Index(name = "idx_payroll_commission_employee", columnList = "com_id, emp_id, status, commission_id"),
        @Index(name = "idx_payroll_commission_project", columnList = "com_id, project_id, commission_id"),
        @Index(name = "idx_payroll_commission_store", columnList = "com_id, store_id, commission_id")
})
public class CommissionItem extends AuditModel {

    @Id
    @SnowflakeId
    @Note("提成明细索引")
    private Long commissionId;

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
    @JoinColumn(name = "payroll_item_id")
    private PayrollItem payrollItem;

    @Note("主合同签约额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Note("手工提成比例")
    @Unit("%")
    @Column(precision = 8, scale = 4, nullable = false)
    private BigDecimal manualRate = BigDecimal.ZERO;

    @Note("提成金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Note("已计提金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal accruedAmount = BigDecimal.ZERO;

    @Note("已释放金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Note("已发放金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("提成状态")
    private CommissionStatus status = CommissionStatus.PendingAccrual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_of")
    private CommissionItem adjustmentOf;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getCommissionId() { return commissionId; }
    public void setCommissionId(Long commissionId) { this.commissionId = commissionId; }
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
    public PayrollItem getPayrollItem() { return payrollItem; }
    public void setPayrollItem(PayrollItem payrollItem) { this.payrollItem = payrollItem; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getManualRate() { return manualRate; }
    public void setManualRate(BigDecimal manualRate) { this.manualRate = manualRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getAccruedAmount() { return accruedAmount; }
    public void setAccruedAmount(BigDecimal accruedAmount) { this.accruedAmount = accruedAmount; }
    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public void setReleasedAmount(BigDecimal releasedAmount) { this.releasedAmount = releasedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public CommissionStatus getStatus() { return status; }
    public void setStatus(CommissionStatus status) { this.status = status; }
    public CommissionItem getAdjustmentOf() { return adjustmentOf; }
    public void setAdjustmentOf(CommissionItem adjustmentOf) { this.adjustmentOf = adjustmentOf; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
