package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 双轨对账附件：原账截图、银行流水、成本表等。 */
@Note("双轨对账附件")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "carryover_reconciliation_attachment", indexes = {
        @Index(name = "idx_carryover_recon_attachment", columnList = "com_id, reconciliation_id, attachment_id")
})
public class CarryoverReconciliationAttachment extends AuditModel {

    @Id
    @SnowflakeId
    @Note("附件索引")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    @Note("所属对账批次")
    private CarryoverReconciliation reconciliation;

    @Column(length = 32)
    @Note("附件分类")
    private String category;

    @Column(name = "file_url", length = 1000, nullable = false)
    @Note("文件地址")
    private String fileUrl;

    @Column(name = "original_name", length = 255)
    @Note("原文件名")
    private String originalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    @Note("上传人")
    private Employee uploadedBy;

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverReconciliation getReconciliation() { return reconciliation; }
    public void setReconciliation(CarryoverReconciliation reconciliation) { this.reconciliation = reconciliation; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public Employee getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Employee uploadedBy) { this.uploadedBy = uploadedBy; }
}
