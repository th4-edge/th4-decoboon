package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.model.Project;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** 接管后剩余施工计划确认记录。 */
@Note("接管后施工计划确认")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_plan_confirmation", indexes = {
        @Index(name = "idx_carryover_plan_confirmation_item", columnList = "com_id, item_id, plan_version")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_carryover_plan_confirmation_version",
                columnNames = {"com_id", "item_id", "plan_version"})
})
public class CarryoverPlanConfirmation extends AuditModel {

    @Id
    @SnowflakeId
    @Note("计划确认索引")
    private Long confirmationId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", length = 16, nullable = false)
    @Note("应确认角色")
    private CarryoverConfirmationRole requiredRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmed_by", nullable = false)
    @Note("实际确认人")
    private Employee confirmedBy;

    @Column(nullable = false)
    @Note("是否代确认")
    private Boolean proxy = Boolean.FALSE;

    @Column(name = "plan_version", nullable = false)
    @Note("计划版本")
    private Integer planVersion;

    @Column(name = "confirmed_at", nullable = false)
    @Note("确认时间")
    private LocalDateTime confirmedAt;

    @Column(length = 500)
    @Note("确认摘要")
    private String summary;

    public Long getConfirmationId() { return confirmationId; }
    public void setConfirmationId(Long confirmationId) { this.confirmationId = confirmationId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public CarryoverConfirmationRole getRequiredRole() { return requiredRole; }
    public void setRequiredRole(CarryoverConfirmationRole requiredRole) { this.requiredRole = requiredRole; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public Boolean getProxy() { return proxy; }
    public void setProxy(Boolean proxy) { this.proxy = proxy; }
    public Integer getPlanVersion() { return planVersion; }
    public void setPlanVersion(Integer planVersion) { this.planVersion = planVersion; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
