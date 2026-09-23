package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.time.LocalDate;

/** 合同级施工阶段快照。 */
@Note("合同施工阶段快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_contract_construction_stage", indexes = {
        @Index(name = "idx_contract_construction_stage_company", columnList = "com_id, stage_id"),
        @Index(name = "idx_contract_construction_stage_plan", columnList = "plan_id, sort")
})
public class ContractConstructionStage extends AuditModel {
    @Id @SnowflakeId @Note("施工阶段索引") private Long stageId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) @Note("施工计划") private ContractConstructionPlan plan;
    @Column(length = 64, nullable = false) @Note("阶段编码") private String code;
    @Column(length = 128, nullable = false) @Note("阶段名称") private String name;
    @Column(nullable = false) @Note("排序") private Integer sort = 0;
    @Column(nullable = false) @Note("计划工期天数") private Integer defaultDays = 1;
    @Column(nullable = false) @Note("计划开始") private LocalDate plannedStart;
    @Column(nullable = false) @Note("计划结束") private LocalDate plannedEnd;
    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("验收模式") private StageAcceptanceMode acceptanceMode = StageAcceptanceMode.None;
    @Column(length = 512) @Note("阶段说明") private String description;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_stage_id") @Note("生成的项目阶段") private ProjectStage projectStage;

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContractConstructionPlan getPlan() { return plan; }
    public void setPlan(ContractConstructionPlan plan) { this.plan = plan; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getDefaultDays() { return defaultDays; }
    public void setDefaultDays(Integer defaultDays) { this.defaultDays = defaultDays; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public StageAcceptanceMode getAcceptanceMode() { return acceptanceMode; }
    public void setAcceptanceMode(StageAcceptanceMode acceptanceMode) { this.acceptanceMode = acceptanceMode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectStage getProjectStage() { return projectStage; }
    public void setProjectStage(ProjectStage projectStage) { this.projectStage = projectStage; }
}
