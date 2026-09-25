package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverAttachmentCategory;
import com.th4.edge.decoboon.common.model.AuditModel;
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

/** 承接项目追溯附件。 */
@Note("承接项目附件")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_attachment", indexes = {
        @Index(name = "idx_carryover_attachment_item", columnList = "com_id, item_id, attachment_id")
})
public class CarryoverAttachment extends AuditModel {

    @Note("附件索引")
    @Id
    @SnowflakeId
    private Long attachmentId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属承接项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private CarryoverProject item;

    @Note("附件分类")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CarryoverAttachmentCategory category;

    @Note("文件地址")
    @Column(name = "file_url", length = 512, nullable = false)
    private String fileUrl;

    @Note("原文件名")
    @Column(name = "original_name", length = 256)
    private String originalName;

    @Note("上传人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private Employee uploadedBy;

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public CarryoverAttachmentCategory getCategory() { return category; }
    public void setCategory(CarryoverAttachmentCategory category) { this.category = category; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public Employee getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Employee uploadedBy) { this.uploadedBy = uploadedBy; }
}
