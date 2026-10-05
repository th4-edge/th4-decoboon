package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 单项目业务承接草稿与结果。 */
@Note("单项目业务承接")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "carryover_project", indexes = {
        @Index(name = "idx_carryover_project_company", columnList = "com_id, item_id"),
        @Index(name = "idx_carryover_project_batch", columnList = "com_id, batch_id, item_id"),
        @Index(name = "idx_carryover_project_status", columnList = "com_id, status, item_id"),
        @Index(name = "idx_carryover_project_store", columnList = "com_id, store_id, item_id"),
        @Index(name = "idx_carryover_project_team", columnList = "com_id, team_id, item_id"),
        @Index(name = "idx_carryover_project_owner", columnList = "com_id, owner_id, item_id")
})
public class CarryoverProject extends AuditModel implements ProjectOwned {

    @Note("承接项目索引")
    @Id
    @SnowflakeId
    private Long itemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @Note("所属承接任务")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private CarryoverBatch batch;

    @Note("状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CarryoverProjectStatus status = CarryoverProjectStatus.Draft;

    @Note("草稿版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("客户姓名")
    @Keyword
    @Column(name = "customer_name", length = 64)
    private String customerName;

    @Note("客户手机号")
    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @Note("小区")
    @Column(length = 128)
    private String community;

    @Note("房屋地址")
    @Column(length = 256)
    private String address;

    @Note("面积")
    @Unit("m²")
    @Column(precision = 10, scale = 2)
    private BigDecimal area;

    @Note("项目经理")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Note("设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    private Employee designer;

    @Note("合同编号")
    @Keyword
    @Column(name = "contract_no", length = 64)
    private String contractNo;

    @Note("原始签约金额")
    @Unit("元")
    @Column(name = "signed_amount", precision = 14, scale = 2)
    private BigDecimal signedAmount;

    @Note("当前合同金额")
    @Unit("元")
    @Column(name = "current_amount", precision = 14, scale = 2)
    private BigDecimal currentAmount;

    @Note("签约日期")
    @Column(name = "signed_at")
    private LocalDate signedAt;

    @Note("开工日期")
    @Column(name = "start_date")
    private LocalDate startDate;

    @Note("计划完工日期")
    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    @Note("项目名称")
    @Keyword
    @Column(name = "project_name", length = 128)
    private String projectName;

    @Note("映射后的项目状态")
    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", length = 16)
    private ProjectStatus projectStatus;

    @Note("当前施工阶段")
    @Column(name = "current_stage_name", length = 128)
    private String currentStageName;

    @Note("当前进度")
    @Column(nullable = false)
    private Integer progress = 0;

    @Note("剩余工作摘要")
    @Column(name = "remaining_work", length = 1000)
    private String remainingWork;

    @Note("当前问题摘要")
    @Column(name = "current_issue", length = 1000)
    private String currentIssue;

    @Note("期初未收金额")
    @Unit("元")
    @Column(name = "receivable_outstanding", precision = 14, scale = 2)
    private BigDecimal receivableOutstanding;

    @Note("期初应收到期日")
    @Column(name = "receivable_due_date")
    private LocalDate receivableDueDate;

    @Note("期初应收原单号")
    @Column(name = "receivable_source_no", length = 64)
    private String receivableSourceNo;

    @Note("期初应收已确认")
    @Column(name = "receivable_confirmed", nullable = false)
    private Boolean receivableConfirmed = Boolean.FALSE;

    @Note("期初未付金额")
    @Unit("元")
    @Column(name = "payable_outstanding", precision = 14, scale = 2)
    private BigDecimal payableOutstanding;

    @Note("期初应付到期日")
    @Column(name = "payable_due_date")
    private LocalDate payableDueDate;

    @Note("期初应付收款方")
    @Column(name = "payable_payee_name", length = 128)
    private String payablePayeeName;

    @Note("期初应付原单号")
    @Column(name = "payable_source_no", length = 64)
    private String payableSourceNo;

    @Note("期初应付已确认")
    @Column(name = "payable_confirmed", nullable = false)
    private Boolean payableConfirmed = Boolean.FALSE;

    @Note("接管前成本总额")
    @Unit("元")
    @Column(name = "cost_total", precision = 14, scale = 2)
    private BigDecimal costTotal;

    @Note("接管前成本完整性")
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_status", length = 16, nullable = false)
    private CarryoverCostStatus costStatus = CarryoverCostStatus.Unknown;

    @Note("接管前成本缺失说明")
    @Column(name = "cost_missing_note", length = 500)
    private String costMissingNote;

    @Note("原系统项目 ID")
    @Column(name = "legacy_id", length = 128)
    private String legacyId;

    @Note("原系统项目编号")
    @Column(name = "legacy_project_no", length = 64)
    private String legacyProjectNo;

    @Note("原账口径备注")
    @Column(name = "legacy_account_note", length = 1000)
    private String legacyAccountNote;

    @Note("接管基线报价")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @Note("合同基线")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ProjectContract contract;

    @Note("正式项目")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Note("期初应收")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id")
    private Receivable receivable;

    @Note("期初应付")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_id")
    private Payable payable;

    @Note("承接完成时间")
    private LocalDateTime appliedAt;

    @Note("取消时间")
    private LocalDateTime cancelledAt;

    @Note("取消原因")
    @Column(length = 500)
    private String cancelReason;

    @Note("锁定时间")
    private LocalDateTime lockedAt;

    @Note("首笔业务发生时间")
    private LocalDateTime firstBusinessAt;

    @Note("首笔业务类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "first_business_type", length = 24)
    private CarryoverBusinessType firstBusinessType;

    @Note("接管后施工计划状态")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status", length = 16, nullable = false)
    private CarryoverPlanStatus planStatus = CarryoverPlanStatus.NotStarted;

    @Note("接管后施工计划版本")
    @Column(name = "plan_version", nullable = false)
    private Integer planVersion = 0;

    @Note("接管后施工计划确认时间")
    @Column(name = "plan_confirmed_at")
    private LocalDateTime planConfirmedAt;

    @Note("接管后施工计划确认人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_confirmed_by")
    private Employee planConfirmedBy;

    @Note("锁定操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private Employee lockedBy;

    @Transient
    @Note("只读投影：待补齐事项")
    private List<String> validationBlockers = new ArrayList<>();

    @Transient
    @Note("只读投影：是否可以提交承接")
    private Boolean carryoverReady = Boolean.FALSE;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    @Override public Company getCompany() { return company; }
    @Override public void setCompany(Company company) { this.company = company; }
    @Override public Store getStore() { return store; }
    @Override public void setStore(Store store) { this.store = store; }
    @Override public Team getTeam() { return team; }
    @Override public void setTeam(Team team) { this.team = team; }
    @Override public Employee getOwner() { return owner; }
    @Override public void setOwner(Employee owner) { this.owner = owner; }
    public CarryoverBatch getBatch() { return batch; }
    public void setBatch(CarryoverBatch batch) { this.batch = batch; }
    public CarryoverProjectStatus getStatus() { return status; }
    public void setStatus(CarryoverProjectStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }
    public Employee getDesigner() { return designer; }
    public void setDesigner(Employee designer) { this.designer = designer; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDate signedAt) { this.signedAt = signedAt; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getPlanEndDate() { return planEndDate; }
    public void setPlanEndDate(LocalDate planEndDate) { this.planEndDate = planEndDate; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public ProjectStatus getProjectStatus() { return projectStatus; }
    public void setProjectStatus(ProjectStatus projectStatus) { this.projectStatus = projectStatus; }
    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getRemainingWork() { return remainingWork; }
    public void setRemainingWork(String remainingWork) { this.remainingWork = remainingWork; }
    public String getCurrentIssue() { return currentIssue; }
    public void setCurrentIssue(String currentIssue) { this.currentIssue = currentIssue; }
    public BigDecimal getReceivableOutstanding() { return receivableOutstanding; }
    public void setReceivableOutstanding(BigDecimal receivableOutstanding) { this.receivableOutstanding = receivableOutstanding; }
    public LocalDate getReceivableDueDate() { return receivableDueDate; }
    public void setReceivableDueDate(LocalDate receivableDueDate) { this.receivableDueDate = receivableDueDate; }
    public String getReceivableSourceNo() { return receivableSourceNo; }
    public void setReceivableSourceNo(String receivableSourceNo) { this.receivableSourceNo = receivableSourceNo; }
    public Boolean getReceivableConfirmed() { return receivableConfirmed; }
    public void setReceivableConfirmed(Boolean receivableConfirmed) { this.receivableConfirmed = receivableConfirmed; }
    public BigDecimal getPayableOutstanding() { return payableOutstanding; }
    public void setPayableOutstanding(BigDecimal payableOutstanding) { this.payableOutstanding = payableOutstanding; }
    public LocalDate getPayableDueDate() { return payableDueDate; }
    public void setPayableDueDate(LocalDate payableDueDate) { this.payableDueDate = payableDueDate; }
    public String getPayablePayeeName() { return payablePayeeName; }
    public void setPayablePayeeName(String payablePayeeName) { this.payablePayeeName = payablePayeeName; }
    public String getPayableSourceNo() { return payableSourceNo; }
    public void setPayableSourceNo(String payableSourceNo) { this.payableSourceNo = payableSourceNo; }
    public Boolean getPayableConfirmed() { return payableConfirmed; }
    public void setPayableConfirmed(Boolean payableConfirmed) { this.payableConfirmed = payableConfirmed; }
    public BigDecimal getCostTotal() { return costTotal; }
    public void setCostTotal(BigDecimal costTotal) { this.costTotal = costTotal; }
    public CarryoverCostStatus getCostStatus() { return costStatus; }
    public void setCostStatus(CarryoverCostStatus costStatus) { this.costStatus = costStatus; }
    public String getCostMissingNote() { return costMissingNote; }
    public void setCostMissingNote(String costMissingNote) { this.costMissingNote = costMissingNote; }
    public String getLegacyId() { return legacyId; }
    public void setLegacyId(String legacyId) { this.legacyId = legacyId; }
    public String getLegacyProjectNo() { return legacyProjectNo; }
    public void setLegacyProjectNo(String legacyProjectNo) { this.legacyProjectNo = legacyProjectNo; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Receivable getReceivable() { return receivable; }
    public void setReceivable(Receivable receivable) { this.receivable = receivable; }
    public Payable getPayable() { return payable; }
    public void setPayable(Payable payable) { this.payable = payable; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public String getLegacyAccountNote() { return legacyAccountNote; }
    public void setLegacyAccountNote(String legacyAccountNote) { this.legacyAccountNote = legacyAccountNote; }

    public LocalDateTime getFirstBusinessAt() { return firstBusinessAt; }
    public void setFirstBusinessAt(LocalDateTime firstBusinessAt) { this.firstBusinessAt = firstBusinessAt; }
    public CarryoverBusinessType getFirstBusinessType() { return firstBusinessType; }
    public void setFirstBusinessType(CarryoverBusinessType firstBusinessType) { this.firstBusinessType = firstBusinessType; }
    public Employee getLockedBy() { return lockedBy; }
    public void setLockedBy(Employee lockedBy) { this.lockedBy = lockedBy; }

    public CarryoverPlanStatus getPlanStatus() { return planStatus; }
    public void setPlanStatus(CarryoverPlanStatus planStatus) { this.planStatus = planStatus; }
    public Integer getPlanVersion() { return planVersion; }
    public void setPlanVersion(Integer planVersion) { this.planVersion = planVersion; }
    public LocalDateTime getPlanConfirmedAt() { return planConfirmedAt; }
    public void setPlanConfirmedAt(LocalDateTime planConfirmedAt) { this.planConfirmedAt = planConfirmedAt; }
    public Employee getPlanConfirmedBy() { return planConfirmedBy; }
    public void setPlanConfirmedBy(Employee planConfirmedBy) { this.planConfirmedBy = planConfirmedBy; }
    public List<String> getValidationBlockers() { return validationBlockers; }
    public void setValidationBlockers(List<String> validationBlockers) { this.validationBlockers = validationBlockers; }
    public Boolean getCarryoverReady() { return carryoverReady; }
    public void setCarryoverReady(Boolean carryoverReady) { this.carryoverReady = carryoverReady; }
}
