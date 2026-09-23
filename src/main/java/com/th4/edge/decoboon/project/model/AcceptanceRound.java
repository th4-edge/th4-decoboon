package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.AcceptanceRectificationType;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 阶段验收整改轮次（append-only）。
 *
 * <p>依据用户 2026-09-21 裁决：整改对象为阶段级、不合格复用同一阶段与同一张验收单、
 * 允许多次循环不限次数、原验收记录保留并追加结论、只做质量事实不碰账。
 * 每次「提交验收 / 提交复验」追加一行，业主确认或驳回时回填本轮结论；历史轮次不可修改。</p>
 */
@Note("阶段验收轮次")
@Description("阶段验收与整改复验的追加式轮次记录，保留每轮结论与整改证据")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_acceptance_round",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_acceptance_round_no", columnNames = {"acceptance_id", "round_no"})
        },
        indexes = {
                @Index(name = "idx_acceptance_round_company", columnList = "com_id, round_id"),
                @Index(name = "idx_acceptance_round_company_acceptance", columnList = "com_id, acceptance_id, round_id"),
                @Index(name = "idx_acceptance_round_company_project", columnList = "com_id, project_id, round_id"),
                @Index(name = "idx_acceptance_round_company_stage", columnList = "com_id, stage_id, round_id"),
                @Index(name = "idx_acceptance_round_company_store", columnList = "com_id, store_id, round_id"),
                @Index(name = "idx_acceptance_round_company_team", columnList = "com_id, team_id, round_id"),
                @Index(name = "idx_acceptance_round_company_owner", columnList = "com_id, owner_id, round_id")
        })
public class AcceptanceRound extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("轮次索引")
    private Long roundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("归属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("归属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("经营负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_id", nullable = false)
    @Note("所属阶段验收单（阶段级唯一）")
    private Acceptance acceptance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    @Note("所属施工阶段（整改唯一归属层级）")
    private ProjectStage stage;

    @Column(name = "round_no", nullable = false)
    @Note("轮次号（同阶段从 1 递增，不限次数、不跳号）")
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("本轮结果：待确认 / 已确认 / 整改中（驳回）")
    private AcceptStatus result = AcceptStatus.Pending;

    @Lob
    @Column
    @Note("本轮整改说明（第 2 轮起必填，说明针对上一轮问题做了什么）")
    private String rectificationNote;

    @ElementCollection
    @CollectionTable(name = "db_acceptance_round_rectification_evidence",
            joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "url", length = 512)
    @Note("整改后照片")
    private List<String> rectificationEvidenceUrls;

    @ElementCollection
    @CollectionTable(name = "db_acceptance_round_evidence", joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "url", length = 512)
    @Note("本轮现场证据")
    private List<String> evidenceUrls;

    @ElementCollection
    @CollectionTable(name = "db_acceptance_round_issue", joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "issue", length = 512)
    @Note("本轮业主提出的问题清单")
    private List<String> issues;

    @Column(length = 1024)
    @Note("本轮结论文字")
    private String conclusion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    @Note("提交人")
    private Employee submittedBy;

    @Note("提交时间")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    @Note("本轮业主确认或驳回操作人")
    private Credentials confirmedBy;

    @Note("本轮确认或驳回时间")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rectification_owner_id")
    @Note("驳回时指定的整改责任人（作用于下一轮）")
    private Employee rectificationOwner;

    @Note("驳回时指定的整改截止日期")
    private LocalDate rectificationDueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("驳回时系统按类型设置的整改类型")
    private AcceptanceRectificationType rectificationType;

    @ElementCollection
    @CollectionTable(name = "db_acceptance_round_rectification_issue_image",
            joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "url", length = 512)
    @Note("本轮业主提出的整改问题图片")
    private List<String> rectificationIssueImages;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    @Override
    public Company getCompany() {
        return company;
    }

    @Override
    public void setCompany(Company company) {
        this.company = company;
    }

    @Override
    public Store getStore() {
        return store;
    }

    @Override
    public void setStore(Store store) {
        this.store = store;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public void setTeam(Team team) {
        this.team = team;
    }

    @Override
    public Employee getOwner() {
        return owner;
    }

    @Override
    public void setOwner(Employee owner) {
        this.owner = owner;
    }

    public Acceptance getAcceptance() {
        return acceptance;
    }

    public void setAcceptance(Acceptance acceptance) {
        this.acceptance = acceptance;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public ProjectStage getStage() {
        return stage;
    }

    public void setStage(ProjectStage stage) {
        this.stage = stage;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public AcceptStatus getResult() {
        return result;
    }

    public void setResult(AcceptStatus result) {
        this.result = result;
    }

    public String getRectificationNote() {
        return rectificationNote;
    }

    public void setRectificationNote(String rectificationNote) {
        this.rectificationNote = rectificationNote;
    }

    public List<String> getRectificationEvidenceUrls() {
        return rectificationEvidenceUrls;
    }

    public void setRectificationEvidenceUrls(List<String> rectificationEvidenceUrls) {
        this.rectificationEvidenceUrls = rectificationEvidenceUrls;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public Employee getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Employee submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Credentials getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Credentials confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Employee getRectificationOwner() {
        return rectificationOwner;
    }

    public void setRectificationOwner(Employee rectificationOwner) {
        this.rectificationOwner = rectificationOwner;
    }

    public LocalDate getRectificationDueDate() {
        return rectificationDueDate;
    }

    public void setRectificationDueDate(LocalDate rectificationDueDate) {
        this.rectificationDueDate = rectificationDueDate;
    }

    public AcceptanceRectificationType getRectificationType() {
        return rectificationType;
    }

    public void setRectificationType(AcceptanceRectificationType rectificationType) {
        this.rectificationType = rectificationType;
    }

    public List<String> getRectificationIssueImages() {
        return rectificationIssueImages;
    }

    public void setRectificationIssueImages(List<String> rectificationIssueImages) {
        this.rectificationIssueImages = rectificationIssueImages;
    }
}
