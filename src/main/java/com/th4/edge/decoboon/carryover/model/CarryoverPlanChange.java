package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanAction;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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

/** 接管后剩余施工计划调整留痕。 */
@Note("接管后施工计划调整记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_plan_change", indexes = {
        @Index(name = "idx_carryover_plan_change_item", columnList = "com_id, item_id, change_id"),
        @Index(name = "idx_carryover_plan_change_project", columnList = "com_id, project_id, change_id")
})
public class CarryoverPlanChange extends AuditModel {

    @Id
    @SnowflakeId
    @Note("调整记录索引")
    private Long changeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @Note("所属承接项目")
    private CarryoverProject item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("正式项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    @Note("涉及的施工阶段；删除后为空")
    private ProjectStage stage;

    @Column(name = "stage_name", length = 128)
    @Note("阶段名称快照")
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("动作")
    private CarryoverPlanAction action;

    @Column(name = "before_snapshot", length = 2000)
    @Note("调整前快照")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", length = 2000)
    @Note("调整后快照")
    private String afterSnapshot;

    @Column(length = 500)
    @Note("调整原因")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    @Note("操作人")
    private Employee createdBy;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public CarryoverPlanAction getAction() { return action; }
    public void setAction(CarryoverPlanAction action) { this.action = action; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterSnapshot() { return afterSnapshot; }
    public void setAfterSnapshot(String afterSnapshot) { this.afterSnapshot = afterSnapshot; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
