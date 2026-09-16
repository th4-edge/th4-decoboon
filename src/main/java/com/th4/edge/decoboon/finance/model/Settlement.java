package com.th4.edge.decoboon.finance.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 项目财务结算快照。 */
@Note("项目结算")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_finance_settlement", indexes = {
        @Index(name = "idx_fin_settlement_company", columnList = "com_id, settlement_id"),
        @Index(name = "idx_fin_settlement_project", columnList = "com_id, project_id, settlement_id"),
        @Index(name = "idx_fin_settlement_store", columnList = "com_id, store_id, settlement_id"),
        @Index(name = "idx_fin_settlement_team", columnList = "com_id, team_id, settlement_id"),
        @Index(name = "idx_fin_settlement_owner", columnList = "com_id, owner_id, settlement_id")
})
public class Settlement extends AuditModel {

    @Note("结算索引")
    @Id
    @SnowflakeId
    private Long settlementId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("归属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("归属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("数据负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Note("合同金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Note("变更金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Note("扣款金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Note("奖励金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal rewardAmount = BigDecimal.ZERO;

    @Note("最终结算金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Note("质保金比例")
    @Unit("%")
    @Column(precision = 6, scale = 2, nullable = false)
    private BigDecimal retentionRate = BigDecimal.ZERO;

    @Note("质保金金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal retentionAmount = BigDecimal.ZERO;

    @Note("质保到期日")
    private LocalDate retentionReleaseAt;

    @Note("质保释放日")
    private LocalDate retentionReleasedAt;

    @Note("应收总额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal receivableAmount = BigDecimal.ZERO;

    @Note("已收总额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Note("应付总额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Note("已付总额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Note("材料款")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal materialAmount = BigDecimal.ZERO;

    @Note("工人工资")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal salaryAmount = BigDecimal.ZERO;

    @Note("项目款")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal projectAmount = BigDecimal.ZERO;

    @Note("费用分摊")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    @Note("项目总成本")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Note("项目毛利")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal profitAmount = BigDecimal.ZERO;

    @Note("毛利率")
    @Unit("%")
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal grossMargin = BigDecimal.ZERO;

    @Note("结算状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SettlementStatus status = SettlementStatus.Draft;

    @Note("确认人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirm_emp_id")
    private Employee confirmedBy;

    @Note("确认日期")
    private LocalDate confirmedAt;

    @Note("关闭日期")
    private LocalDate closedAt;

    @Note("结算说明")
    @Column(length = 500)
    private String remark;

    @Note("创建人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(BigDecimal rewardAmount) { this.rewardAmount = rewardAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public BigDecimal getRetentionRate() { return retentionRate; }
    public void setRetentionRate(BigDecimal retentionRate) { this.retentionRate = retentionRate; }
    public BigDecimal getRetentionAmount() { return retentionAmount; }
    public void setRetentionAmount(BigDecimal retentionAmount) { this.retentionAmount = retentionAmount; }
    public LocalDate getRetentionReleaseAt() { return retentionReleaseAt; }
    public void setRetentionReleaseAt(LocalDate retentionReleaseAt) { this.retentionReleaseAt = retentionReleaseAt; }
    public LocalDate getRetentionReleasedAt() { return retentionReleasedAt; }
    public void setRetentionReleasedAt(LocalDate retentionReleasedAt) { this.retentionReleasedAt = retentionReleasedAt; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getMaterialAmount() { return materialAmount; }
    public void setMaterialAmount(BigDecimal materialAmount) { this.materialAmount = materialAmount; }
    public BigDecimal getSalaryAmount() { return salaryAmount; }
    public void setSalaryAmount(BigDecimal salaryAmount) { this.salaryAmount = salaryAmount; }
    public BigDecimal getProjectAmount() { return projectAmount; }
    public void setProjectAmount(BigDecimal projectAmount) { this.projectAmount = projectAmount; }
    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDate getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDate confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDate getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDate closedAt) { this.closedAt = closedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
