package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.plan.configure.PlanSnapshotStatus;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

/** 合同级施工计划快照。 */
@Note("合同施工计划快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_contract_construction_plan", uniqueConstraints = @UniqueConstraint(
        name = "uk_contract_construction_plan_contract", columnNames = "contract_id"))
public class ContractConstructionPlan extends AuditModel {
    @Id @SnowflakeId @Note("施工计划索引") private Long planId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contract_id", nullable = false) @Note("所属合同") private ProjectContract contract;
    @Column(length = 64, nullable = false) @Note("基线模板编码") private String sourceCode;
    @Column(nullable = false) @Note("基线模板版本") private Integer sourceVersion = 1;
    @Column(length = 128, nullable = false) @Note("模板名称") private String name;
    @Column(nullable = false) @Note("计划开工") private LocalDate startDate;
    @Column(nullable = false) @Note("计划竣工") private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("快照状态") private PlanSnapshotStatus status = PlanSnapshotStatus.Confirmed;
    @Transient @Note("施工阶段") private List<ContractConstructionStage> stages = List.of();

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public Integer getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(Integer sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PlanSnapshotStatus getStatus() { return status; }
    public void setStatus(PlanSnapshotStatus status) { this.status = status; }
    public List<ContractConstructionStage> getStages() { return stages; }
    public void setStages(List<ContractConstructionStage> stages) { this.stages = stages == null ? List.of() : List.copyOf(stages); }
}
