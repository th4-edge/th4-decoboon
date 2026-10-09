package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** 员工内容任务执行记录。 */
@Note("员工内容执行记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_employee_content", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_employee_task", columnNames = {"task_id", "emp_id"})
}, indexes = {
        @Index(name = "idx_content_employee_company", columnList = "com_id, content_id"),
        @Index(name = "idx_content_employee_employee", columnList = "emp_id, status, content_id")
})
public class EmployeeContent extends AuditModel {

    @Id @SnowflakeId @Note("员工内容索引")
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false) @Note("内容任务")
    private ContentTask task;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", nullable = false) @Note("员工")
    private Employee employee;

    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("执行状态")
    private EmployeeContentStatus status = EmployeeContentStatus.NotStarted;

    @Column(name = "started_at") @Note("领取时间")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at") @Note("提交时间")
    private LocalDateTime submittedAt;

    @Column(name = "review_status", length = 32) @Note("审核状态")
    private String reviewStatus;

    @Column(name = "publish_status", length = 32) @Note("发布状态")
    private String publishStatus;

    @Column(name = "rejection_reason", length = 500) @Note("驳回原因")
    private String rejectionReason;

    @Column(name = "selected_asset_ids", columnDefinition = "TEXT") @Note("选中素材ID列表")
    private String selectedAssetIds;

    @Column(columnDefinition = "TEXT") @Note("发布文案")
    private String caption;

    @Column(name = "rendered_file_path", length = 512) @Note("成片文件路径")
    private String renderedFilePath;

    @Column(name = "cover_file_path", length = 512) @Note("封面文件路径")
    private String coverFilePath;

    @Column(name = "render_job_id", length = 128) @Note("渲染任务编号")
    private String renderJobId;

    @Column(name = "render_error", length = 500) @Note("渲染错误")
    private String renderError;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") @Note("审核人")
    private Employee reviewedBy;

    @Column(name = "reviewed_at") @Note("审核时间")
    private LocalDateTime reviewedAt;

    @Transient @Note("成片短期地址")
    private String renderedUrl;

    @Transient @Note("封面短期地址")
    private String coverUrl;

    @Transient @Note("最近发布记录")
    private PublicationRecord latestPublication;

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContentTask getTask() { return task; }
    public void setTask(ContentTask task) { this.task = task; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public EmployeeContentStatus getStatus() { return status; }
    public void setStatus(EmployeeContentStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getSelectedAssetIds() { return selectedAssetIds; }
    public void setSelectedAssetIds(String selectedAssetIds) { this.selectedAssetIds = selectedAssetIds; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getRenderedFilePath() { return renderedFilePath; }
    public void setRenderedFilePath(String renderedFilePath) { this.renderedFilePath = renderedFilePath; }
    public String getCoverFilePath() { return coverFilePath; }
    public void setCoverFilePath(String coverFilePath) { this.coverFilePath = coverFilePath; }
    public String getRenderJobId() { return renderJobId; }
    public void setRenderJobId(String renderJobId) { this.renderJobId = renderJobId; }
    public String getRenderError() { return renderError; }
    public void setRenderError(String renderError) { this.renderError = renderError; }
    public Employee getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Employee reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRenderedUrl() { return renderedUrl; }
    public void setRenderedUrl(String renderedUrl) { this.renderedUrl = renderedUrl; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public PublicationRecord getLatestPublication() { return latestPublication; }
    public void setLatestPublication(PublicationRecord latestPublication) { this.latestPublication = latestPublication; }
}
