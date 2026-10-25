package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.ExpenseDocumentType;
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

/** 费用证据附件；只用于业务留证，不承担发票管理。 */
@Note("费用证据附件")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_finance_expense_attachment", indexes = {
        @Index(name = "idx_fin_expense_attachment_company", columnList = "com_id, attachment_id"),
        @Index(name = "idx_fin_expense_attachment_expense", columnList = "com_id, expense_id, attachment_id")
})
public class ExpenseAttachment extends AuditModel {

    @Id
    @SnowflakeId
    @Note("附件索引")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    @Note("所属费用")
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 32, nullable = false)
    @Note("证据类型")
    private ExpenseDocumentType documentType = ExpenseDocumentType.Other;

    @Column(name = "file_url", length = 512, nullable = false)
    @Note("文件存储路径")
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    @Note("文件原始名称")
    private String fileName;

    @Column(length = 64)
    @Note("文件 SHA-256；上传能力可提供时记录")
    private String sha256;

    @Column(name = "file_size")
    @Note("文件字节数")
    private Long size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    @Note("上传人")
    private Employee uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    @Note("上传时间")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public ExpenseDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(ExpenseDocumentType documentType) { this.documentType = documentType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Employee getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Employee uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
