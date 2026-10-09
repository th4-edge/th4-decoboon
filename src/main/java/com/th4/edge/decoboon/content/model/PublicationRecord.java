package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentPlatform;
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
import java.time.LocalDateTime;

/** 员工内容发布记录。 */
@Note("内容发布记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_publication", indexes = {
        @Index(name = "idx_content_publication_company", columnList = "com_id, publication_id"),
        @Index(name = "idx_content_publication_content", columnList = "employee_content_id, publication_id")
})
public class PublicationRecord extends AuditModel {

    @Id @SnowflakeId @Note("发布记录索引")
    private Long publicationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_content_id", nullable = false) @Note("员工内容")
    private EmployeeContent employeeContent;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false) @Note("内容任务")
    private ContentTask task;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", nullable = false) @Note("发布员工")
    private Employee employee;

    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("发布平台")
    private ContentPlatform platform = ContentPlatform.Douyin;

    @Column(name = "post_url", length = 1000) @Note("作品链接")
    private String postUrl;

    @Column(name = "platform_post_id", length = 128) @Note("平台作品ID")
    private String platformPostId;

    @Column(name = "published_at") @Note("发布时间")
    private LocalDateTime publishedAt;

    @Column(name = "backfill_evidence_path", length = 512) @Note("回填证据文件")
    private String backfillEvidencePath;

    @Column(length = 24, nullable = false) @Note("回填状态")
    private String status = "Backfilled";

    public Long getPublicationId() { return publicationId; }
    public void setPublicationId(Long publicationId) { this.publicationId = publicationId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public EmployeeContent getEmployeeContent() { return employeeContent; }
    public void setEmployeeContent(EmployeeContent employeeContent) { this.employeeContent = employeeContent; }
    public ContentTask getTask() { return task; }
    public void setTask(ContentTask task) { this.task = task; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public ContentPlatform getPlatform() { return platform; }
    public void setPlatform(ContentPlatform platform) { this.platform = platform; }
    public String getPostUrl() { return postUrl; }
    public void setPostUrl(String postUrl) { this.postUrl = postUrl; }
    public String getPlatformPostId() { return platformPostId; }
    public void setPlatformPostId(String platformPostId) { this.platformPostId = platformPostId; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getBackfillEvidencePath() { return backfillEvidencePath; }
    public void setBackfillEvidencePath(String backfillEvidencePath) { this.backfillEvidencePath = backfillEvidencePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
