package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.PlanDeviationReasonType;
import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.configure.PlanDeviationStrategy;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划偏差单。
 *
 * <p>DEC-051：合同基准完工日锁定后，任务排期产生的“预计完工日 &gt; 合同基准完工日”形成计划偏差，
 * 任何超窗都生成。确认人 Boss/店长；顺延方式由发起人选择，Boss/店长最终确认生效；
 * 客户原因（改方案、停工）强制走合同工期变更。</p>
 */
@Note("计划偏差单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project_plan_deviation", indexes = {
        @Index(name = "idx_plan_deviation_company", columnList = "com_id, deviation_id"),
        @Index(name = "idx_plan_deviation_company_store", columnList = "com_id, store_id, deviation_id"),
        @Index(name = "idx_plan_deviation_company_team", columnList = "com_id, team_id, deviation_id"),
        @Index(name = "idx_plan_deviation_company_owner", columnList = "com_id, owner_id, deviation_id"),
        @Index(name = "idx_plan_deviation_company_project_status", columnList = "com_id, project_id, status, deviation_id")
})
public class ProjectPlanDeviation extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("偏差单索引")
    private Long deviationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("所属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("经营负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    @Note("关联合同")
    private ProjectContract contract;

    @Column(name = "base_end_date", nullable = false)
    @Note("合同基准完工日（发起时快照）")
    private LocalDate baseEndDate;

    @Column(name = "expected_end_date", nullable = false)
    @Note("当前预计完工日")
    private LocalDate expectedEndDate;

    @Column(name = "deviation_days")
    @Note("偏差天数")
    private Integer deviationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", length = 16)
    @Note("原因分类")
    private PlanDeviationReasonType reasonType;

    @Column(length = 512)
    @Note("偏差原因说明")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_strategy", length = 16)
    @Note("顺延方式（发起人选择）")
    private PlanDeviationStrategy scheduleStrategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    @Note("发起人/责任人")
    private Employee responsible;

    @Column(name = "due_date")
    @Note("确认截止日期")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("偏差状态")
    private PlanDeviationStatus status = PlanDeviationStatus.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    @Note("确认人")
    private Employee confirmedBy;

    @Column(name = "confirmed_at")
    @Note("确认时间")
    private LocalDateTime confirmedAt;

    @Column(name = "new_base_end_date")
    @Note("确认后新的合同基准完工日")
    private LocalDate newBaseEndDate;

    @Column(name = "receivable_recalculated", nullable = false)
    @Note("是否已重算应收触发日")
    private Boolean receivableRecalculated = Boolean.FALSE;

    @Column(name = "visible_to_customer", nullable = false)
    @Note("业主是否可见")
    private Boolean visibleToCustomer = Boolean.TRUE;

    @Override
    public Company getCompany() { return company; }

    @Override
    public void setCompany(Company company) { this.company = company; }

    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public Employee getOwner() { return owner; }

    @Override
    public void setOwner(Employee owner) { this.owner = owner; }

    public Long getDeviationId() { return deviationId; }

    public void setDeviationId(Long deviationId) { this.deviationId = deviationId; }

    public Project getProject() { return project; }

    public void setProject(Project project) { this.project = project; }

    public Property getProperty() { return property; }

    public void setProperty(Property property) { this.property = property; }

    public ProjectContract getContract() { return contract; }

    public void setContract(ProjectContract contract) { this.contract = contract; }

    public LocalDate getBaseEndDate() { return baseEndDate; }

    public void setBaseEndDate(LocalDate baseEndDate) { this.baseEndDate = baseEndDate; }

    public LocalDate getExpectedEndDate() { return expectedEndDate; }

    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }

    public Integer getDeviationDays() { return deviationDays; }

    public void setDeviationDays(Integer deviationDays) { this.deviationDays = deviationDays; }

    public PlanDeviationReasonType getReasonType() { return reasonType; }

    public void setReasonType(PlanDeviationReasonType reasonType) { this.reasonType = reasonType; }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }

    public PlanDeviationStrategy getScheduleStrategy() { return scheduleStrategy; }

    public void setScheduleStrategy(PlanDeviationStrategy scheduleStrategy) { this.scheduleStrategy = scheduleStrategy; }

    public Employee getResponsible() { return responsible; }

    public void setResponsible(Employee responsible) { this.responsible = responsible; }

    public LocalDate getDueDate() { return dueDate; }

    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public PlanDeviationStatus getStatus() { return status; }

    public void setStatus(PlanDeviationStatus status) { this.status = status; }

    public Employee getConfirmedBy() { return confirmedBy; }

    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }

    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDate getNewBaseEndDate() { return newBaseEndDate; }

    public void setNewBaseEndDate(LocalDate newBaseEndDate) { this.newBaseEndDate = newBaseEndDate; }

    public Boolean getReceivableRecalculated() { return receivableRecalculated; }

    public void setReceivableRecalculated(Boolean receivableRecalculated) { this.receivableRecalculated = receivableRecalculated; }

    public Boolean getVisibleToCustomer() { return visibleToCustomer; }

    public void setVisibleToCustomer(Boolean visibleToCustomer) { this.visibleToCustomer = visibleToCustomer; }
}
