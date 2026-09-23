package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
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
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;

/**
 * 项目施工阶段。
 *
 * <p>阶段来源为 BIM 工程分类 Modular。阶段进度和状态由阶段内施工任务汇总，
 * 不在前端保存时直接接受进度值。</p>
 */
@Note("项目施工阶段")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project_stage",
        indexes = {
                @Index(name = "idx_project_stage_company", columnList = "com_id, stage_id"),
                @Index(name = "idx_project_stage_company_store", columnList = "com_id, store_id, stage_id"),
                @Index(name = "idx_project_stage_company_team", columnList = "com_id, team_id, stage_id"),
                @Index(name = "idx_project_stage_company_owner", columnList = "com_id, owner_id, stage_id"),
                @Index(name = "idx_project_stage_company_code", columnList = "com_id, stage_code")
        }
)
public class ProjectStage extends AuditModel implements ProjectOwned {

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



    @Note("阶段索引")
    @Id
    @SnowflakeId
    private Long stageId;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("阶段名称")
    @Column(length = 128, nullable = false)
    private String name;

    @Note("稳定阶段编码")
    @Column(name = "stage_code", length = 64)
    private String stageCode;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("阶段权重")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal weight = BigDecimal.ZERO;

    @Note("阶段状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private StageStatus status = StageStatus.Todo;

    @Note("阶段进度")
    @LogicField
    @Column(nullable = false)
    private Integer progress = 0;

    @Note("计划开始日期")
    private LocalDate plannedStart;

    @Note("计划结束日期")
    private LocalDate plannedEnd;

    @Note("实际开始日期")
    private LocalDate actualStart;

    @Note("实际结束日期")
    private LocalDate actualEnd;

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public StageStatus getStatus() {
        return status;
    }

    public void setStatus(StageStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDate getPlannedStart() {
        return plannedStart;
    }

    public void setPlannedStart(LocalDate plannedStart) {
        this.plannedStart = plannedStart;
    }

    public LocalDate getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(LocalDate plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public LocalDate getActualStart() {
        return actualStart;
    }

    public void setActualStart(LocalDate actualStart) {
        this.actualStart = actualStart;
    }

    public LocalDate getActualEnd() {
        return actualEnd;
    }

    public void setActualEnd(LocalDate actualEnd) {
        this.actualEnd = actualEnd;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
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


}
