package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentPlatform;
import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
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
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

/** 内容任务。 */
@Note("内容任务")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_task", indexes = {
        @Index(name = "idx_content_task_company", columnList = "com_id, task_id"),
        @Index(name = "idx_content_task_company_status", columnList = "com_id, status, task_id")
})
public class ContentTask extends AuditModel {

    @Id @SnowflakeId @Note("任务索引")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id", nullable = false) @Note("内容模板")
    private ContentTemplate template;

    @Keyword @Column(length = 255, nullable = false) @Note("任务标题")
    private String title;

    @Column(length = 32, nullable = false) @Note("传播目标")
    private String goal;

    @Column(name = "target_audience", length = 500, nullable = false) @Note("目标人群")
    private String targetAudience;

    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("发布平台")
    private ContentPlatform platform = ContentPlatform.Douyin;

    @Column(name = "material_ids", columnDefinition = "TEXT") @Note("素材ID列表，逗号分隔")
    private String materialIds;

    @Column(name = "required_claims", columnDefinition = "TEXT") @Note("必须表达")
    private String requiredClaims;

    @Column(name = "forbidden_claims", columnDefinition = "TEXT") @Note("禁止表达")
    private String forbiddenClaims;

    @Column(nullable = false) @Note("截止时间")
    private LocalDateTime deadline;

    @Column(name = "assignee_scope", length = 32, nullable = false) @Note("分配范围")
    private String assigneeScope = "Company";

    @Column(name = "assignee_emp_ids", columnDefinition = "TEXT") @Note("指定员工ID列表")
    private String assigneeEmpIds;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id", nullable = false) @Note("审核人")
    private Employee reviewer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_owner_id", nullable = false) @Note("线索承接人")
    private Employee leadOwner;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false) @Note("创建人")
    private Employee createdBy;

    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("任务状态")
    private ContentTaskStatus status = ContentTaskStatus.Draft;

    @Column(name = "published_at") @Note("发布时间")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at") @Note("关闭时间")
    private LocalDateTime closedAt;

    @Transient @Note("当前员工是否已领取")
    private Boolean claimedByMe = false;

    @Transient @Note("当前员工执行状态")
    private String myContentStatus;

    @Transient @Note("当前员工内容记录索引")
    private Long myContentId;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public ContentTemplate getTemplate() { return template; }
    public void setTemplate(ContentTemplate template) { this.template = template; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public ContentPlatform getPlatform() { return platform; }
    public void setPlatform(ContentPlatform platform) { this.platform = platform; }
    public String getMaterialIds() { return materialIds; }
    public void setMaterialIds(String materialIds) { this.materialIds = materialIds; }
    public String getRequiredClaims() { return requiredClaims; }
    public void setRequiredClaims(String requiredClaims) { this.requiredClaims = requiredClaims; }
    public String getForbiddenClaims() { return forbiddenClaims; }
    public void setForbiddenClaims(String forbiddenClaims) { this.forbiddenClaims = forbiddenClaims; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getAssigneeScope() { return assigneeScope; }
    public void setAssigneeScope(String assigneeScope) { this.assigneeScope = assigneeScope; }
    public String getAssigneeEmpIds() { return assigneeEmpIds; }
    public void setAssigneeEmpIds(String assigneeEmpIds) { this.assigneeEmpIds = assigneeEmpIds; }
    public Employee getReviewer() { return reviewer; }
    public void setReviewer(Employee reviewer) { this.reviewer = reviewer; }
    public Employee getLeadOwner() { return leadOwner; }
    public void setLeadOwner(Employee leadOwner) { this.leadOwner = leadOwner; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public ContentTaskStatus getStatus() { return status; }
    public void setStatus(ContentTaskStatus status) { this.status = status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Boolean getClaimedByMe() { return claimedByMe; }
    public void setClaimedByMe(Boolean claimedByMe) { this.claimedByMe = claimedByMe; }
    public String getMyContentStatus() { return myContentStatus; }
    public void setMyContentStatus(String myContentStatus) { this.myContentStatus = myContentStatus; }
    public Long getMyContentId() { return myContentId; }
    public void setMyContentId(Long myContentId) { this.myContentId = myContentId; }
}
