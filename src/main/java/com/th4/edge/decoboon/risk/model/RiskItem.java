package com.th4.edge.decoboon.risk.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.risk.configure.RiskStatus;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.framework.core.annotation.Description;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 风险处置单（当前处理态）。
 *
 * <p>与经营预警的关系：预警由 {@code BusinessAlertContext} 实时计算、不落库；
 * 本实体只保存「人对该预警的处置事实」，用稳定键 {@code alertKey} 关联，
 * <strong>不复制预警内容</strong>，等级与类型仅作筛选冗余。</p>
 *
 * <p>依据用户 2026-09-23 裁决：</p>
 * <ul>
 *   <li>同一企业同一 {@code alertKey} 唯一，重开只增轮次不新建单；</li>
 *   <li>同级复现不自动重开，仅累计 {@code recurrenceCount}；</li>
 *   <li>级别较创建时升级才自动重开；</li>
 *   <li>处置单不产生或修改任何账务事实。</li>
 * </ul>
 */
@Note("风险处置单")
@Description("经营预警的处置轨迹：责任人、状态、时效、轮次与复现次数")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_risk_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_risk_item_alert_key", columnNames = {"com_id", "alert_key"})
        },
        indexes = {
                @Index(name = "idx_risk_item_company", columnList = "com_id, risk_id"),
                @Index(name = "idx_risk_item_company_store", columnList = "com_id, store_id, risk_id"),
                @Index(name = "idx_risk_item_company_team", columnList = "com_id, team_id, risk_id"),
                @Index(name = "idx_risk_item_company_owner", columnList = "com_id, owner_id, risk_id"),
                @Index(name = "idx_risk_item_company_status_due", columnList = "com_id, status, due_at"),
                @Index(name = "idx_risk_item_company_project", columnList = "com_id, project_id, risk_id")
        })
public class RiskItem extends AuditModel {

    @Id
    @SnowflakeId
    @Note("风险处置单索引")
    private Long riskId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Note("当前处置责任人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @Note("稳定预警键：alertType:businessType:businessId（跨周期不变）")
    @Column(name = "alert_key", length = 160, nullable = false)
    private String alertKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 40, nullable = false)
    @Note("预警类型（冗余，仅用于筛选与索引）")
    private BusinessAlertType alertType;

    @Note("关联业务对象类型")
    @Column(name = "business_type", length = 40)
    private String businessType;

    @Note("关联业务对象主键")
    @Column(name = "business_id")
    private Long businessId;

    @Note("关联项目（可空）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_at_create", length = 20)
    @Note("首次出现时的级别快照，用于判断是否升级")
    private BusinessAlertLevel levelAtCreate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Note("处置状态")
    private RiskStatus status = RiskStatus.Pending;

    @Note("首次发现时间")
    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Note("最近一次指派时间")
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Note("应处置截止时间（按级别 SLA 计算）")
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Note("关闭时间")
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    @Note("关闭人")
    private Employee closedBy;

    @Note("处置结果说明（关闭必填）")
    @Column(name = "close_result", length = 500)
    private String closeResult;

    @Note("重开轮次，从 1 递增")
    @Column(name = "round_no")
    private Integer roundNo = 1;

    @Note("同级复现次数（已关闭后再次出现且未升级）")
    @Column(name = "recurrence_count")
    private Integer recurrenceCount = 0;

    @Note("最近一次重开原因")
    @Column(name = "reopen_reason", length = 300)
    private String reopenReason;

    public Long getRiskId() { return riskId; }
    public void setRiskId(Long riskId) { this.riskId = riskId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }
    public BusinessAlertType getAlertType() { return alertType; }
    public void setAlertType(BusinessAlertType alertType) { this.alertType = alertType; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public BusinessAlertLevel getLevelAtCreate() { return levelAtCreate; }
    public void setLevelAtCreate(BusinessAlertLevel levelAtCreate) { this.levelAtCreate = levelAtCreate; }
    public RiskStatus getStatus() { return status; }
    public void setStatus(RiskStatus status) { this.status = status; }
    public LocalDateTime getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(LocalDateTime discoveredAt) { this.discoveredAt = discoveredAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Employee getClosedBy() { return closedBy; }
    public void setClosedBy(Employee closedBy) { this.closedBy = closedBy; }
    public String getCloseResult() { return closeResult; }
    public void setCloseResult(String closeResult) { this.closeResult = closeResult; }
    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }
    public Integer getRecurrenceCount() { return recurrenceCount; }
    public void setRecurrenceCount(Integer recurrenceCount) { this.recurrenceCount = recurrenceCount; }
    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }
}
