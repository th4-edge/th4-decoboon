package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.time.LocalDateTime;
import java.util.List;
import com.th4.edge.tenant.user.model.Company;

/**
 * 阶段验收单。
 *
 * <p>验收单只在阶段内施工任务全部完成后创建。客户确认后由领域上下文推进阶段状态，
 * 驳回时保留原因并允许整改后重新提交。</p>
 */
@Note("阶段验收单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_acceptance",
        indexes = {
                @Index(name = "idx_acceptance_company", columnList = "com_id, acceptance_id"),
                @Index(name = "idx_acceptance_company_store", columnList = "com_id, store_id, acceptance_id"),
                @Index(name = "idx_acceptance_company_team", columnList = "com_id, team_id, acceptance_id"),
                @Index(name = "idx_acceptance_company_owner", columnList = "com_id, owner_id, acceptance_id")
        }
)
public class Acceptance extends AuditModel implements ProjectOwned {

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



    @Note("验收单索引")
    @Id
    @SnowflakeId
    private Long acceptanceId;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("验收阶段")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private ProjectStage stage;

    @Note("提交人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private Employee submittedBy;

    @Note("验收状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AcceptStatus status = AcceptStatus.Pending;

    @Note("验收结论")
    @Column(length = 1024)
    private String conclusion;

    @Note("问题清单")
    @ElementCollection
    @CollectionTable(name = "db_decoboon_acceptance_issue", joinColumns = @JoinColumn(name = "acceptance_id"))
    @Column(name = "issue", length = 512)
    private List<String> issues;

    @Note("验收影像")
    @ElementCollection
    @CollectionTable(name = "db_decoboon_acceptance_evidence", joinColumns = @JoinColumn(name = "acceptance_id"))
    @Column(name = "url", length = 512)
    private List<String> evidenceUrls;

    @Note("确认时间")
    private LocalDateTime confirmedAt;

    @Note("确认或驳回操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Credentials confirmedBy;

    public Long getAcceptanceId() {
        return acceptanceId;
    }

    public void setAcceptanceId(Long acceptanceId) {
        this.acceptanceId = acceptanceId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectStage getStage() {
        return stage;
    }

    public void setStage(ProjectStage stage) {
        this.stage = stage;
    }

    public Employee getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Employee submittedBy) {
        this.submittedBy = submittedBy;
    }

    public AcceptStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptStatus status) {
        this.status = status;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Credentials getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Credentials confirmedBy) {
        this.confirmedBy = confirmedBy;
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
