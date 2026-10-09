package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentMediaType;
import com.th4.edge.decoboon.content.configure.ContentSourceType;
import com.th4.edge.decoboon.content.configure.ContentTruthType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
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

/** 内容获客素材。 */
@Note("内容素材")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_asset", indexes = {
        @Index(name = "idx_content_asset_company", columnList = "com_id, asset_id"),
        @Index(name = "idx_content_asset_company_project", columnList = "com_id, project_id, asset_id"),
        @Index(name = "idx_content_asset_company_status", columnList = "com_id, status, asset_id")
})
public class ContentAsset extends AuditModel {

    @Id
    @SnowflakeId
    @Note("素材索引")
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @Note("关联客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    @Note("上传员工")
    private Employee uploader;

    @Keyword
    @Column(length = 255, nullable = false)
    @Note("素材标题")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("媒体类型")
    private ContentMediaType mediaType = ContentMediaType.Image;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("素材来源")
    private ContentSourceType sourceType = ContentSourceType.Camera;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("真实性类型")
    private ContentTruthType truthType = ContentTruthType.Native;

    @Column(name = "file_path", length = 512, nullable = false)
    @Note("私有文件路径")
    private String filePath;

    @Column(name = "original_name", length = 255)
    @Note("原始文件名")
    private String originalName;

    @Column(name = "content_type", length = 128)
    @Note("文件类型")
    private String contentType;

    @Column(name = "file_size")
    @Note("文件大小")
    private Long fileSize;

    @Column(length = 2000)
    @Note("标签，逗号分隔")
    private String tags;

    @Column(nullable = false)
    @Note("是否完成脱敏")
    private Boolean desensitized = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("素材状态")
    private ContentAssetStatus status = ContentAssetStatus.Draft;

    @Column(length = 1000)
    @Note("备注")
    private String remark;

    @Column(nullable = false)
    @Note("数据版本")
    private Integer dataVersion = 1;

    @Transient
    @Note("预览短期地址")
    private String previewUrl;

    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Employee getUploader() { return uploader; }
    public void setUploader(Employee uploader) { this.uploader = uploader; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ContentMediaType getMediaType() { return mediaType; }
    public void setMediaType(ContentMediaType mediaType) { this.mediaType = mediaType; }
    public ContentSourceType getSourceType() { return sourceType; }
    public void setSourceType(ContentSourceType sourceType) { this.sourceType = sourceType; }
    public ContentTruthType getTruthType() { return truthType; }
    public void setTruthType(ContentTruthType truthType) { this.truthType = truthType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Boolean getDesensitized() { return desensitized; }
    public void setDesensitized(Boolean desensitized) { this.desensitized = desensitized; }
    public ContentAssetStatus getStatus() { return status; }
    public void setStatus(ContentAssetStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getDataVersion() { return dataVersion; }
    public void setDataVersion(Integer dataVersion) { this.dataVersion = dataVersion; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
}
