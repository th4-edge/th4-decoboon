package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
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

/** 企业施工计划阶段。 */
@Note("企业施工计划阶段")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_company_construction_stage", indexes = {
        @Index(name = "idx_company_construction_stage_company", columnList = "com_id, stage_id"),
        @Index(name = "idx_company_construction_stage_plan", columnList = "plan_id, sort")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_construction_stage_code", columnNames = {"plan_id", "code"})
})
public class CompanyConstructionStage extends AuditModel {

    @Id
    @SnowflakeId
    @Note("阶段索引")
    private Long stageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @Note("施工计划模板")
    private CompanyConstructionPlan plan;

    @Column(length = 64, nullable = false)
    @Note("阶段编码")
    private String code;

    @Column(length = 128, nullable = false)
    @Note("阶段名称")
    private String name;

    @Column(nullable = false)
    @Note("标准工期天数")
    private Integer defaultDays = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("验收模式")
    private StageAcceptanceMode acceptanceMode = StageAcceptanceMode.None;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    @Column(length = 512)
    @Note("阶段说明")
    private String description;

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CompanyConstructionPlan getPlan() { return plan; }
    public void setPlan(CompanyConstructionPlan plan) { this.plan = plan; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDefaultDays() { return defaultDays; }
    public void setDefaultDays(Integer defaultDays) { this.defaultDays = defaultDays; }
    public StageAcceptanceMode getAcceptanceMode() { return acceptanceMode; }
    public void setAcceptanceMode(StageAcceptanceMode acceptanceMode) { this.acceptanceMode = acceptanceMode; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
