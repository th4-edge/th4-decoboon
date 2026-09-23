package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 不可覆盖的 PDF 归档记录。 */
@Note("PDF归档记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "generatedBy")
@Table(name = "db_document_record", indexes = {
        @Index(name = "idx_document_record_company", columnList = "com_id, document_id"),
        @Index(name = "idx_document_record_quote", columnList = "com_id, quote_id, document_id"),
        @Index(name = "idx_document_record_project", columnList = "com_id, project_id, document_id")
})
public class DocumentRecord extends AuditModel {

    @Id @SnowflakeId @Note("归档索引") private Long documentId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("单据类型") private DocumentType type;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("输出对象") private DocumentAudience audience = DocumentAudience.Customer;
    @Column(length = 64, nullable = false) @Note("业务编号") private String businessNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quote_id") @Note("关联报价") private Quote quote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") @Note("关联项目") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reprint_of_id") @Note("原归档记录") private DocumentRecord reprintOf;
    @Column(nullable = false) @Note("模板版本") private Integer templateVersion;
    @Column(length = 512, nullable = false) @Note("私有对象键") private String path;
    @Column(length = 64, nullable = false) @Note("文件SHA256") private String sha256;
    @Column(length = 256, nullable = false) @Note("文件名") private String fileName;
    @Column(length = 64, nullable = false) @Note("内容类型") private String contentType = "application/pdf";
    @Column(nullable = false) @Note("文件大小") private Long size;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "generated_by_id", nullable = false) @Note("生成人") private Employee generatedBy;
    @Column(nullable = false) @Note("生成时间") private LocalDateTime generatedAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("归档状态") private DocumentStatus status = DocumentStatus.Active;
    @Transient @Note("短期访问地址") private String url;
    @Transient @Note("短期地址过期时间") private LocalDateTime urlExpiresAt;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public DocumentAudience getAudience() { return audience; }
    public void setAudience(DocumentAudience audience) { this.audience = audience; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public DocumentRecord getReprintOf() { return reprintOf; }
    public void setReprintOf(DocumentRecord reprintOf) { this.reprintOf = reprintOf; }
    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Employee getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Employee generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getUrlExpiresAt() { return urlExpiresAt; }
    public void setUrlExpiresAt(LocalDateTime urlExpiresAt) { this.urlExpiresAt = urlExpiresAt; }
}
