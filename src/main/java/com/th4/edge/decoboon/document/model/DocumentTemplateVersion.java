package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.configure.DocumentTemplateVersionStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** 企业单据模板不可变版本。 */
@Note("企业单据模板版本")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_document_template_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_template_version",
                columnNames = {"com_id", "template_id", "version"}),
        indexes = {
                @Index(name = "idx_document_template_version_company", columnList = "com_id, version_id"),
                @Index(name = "idx_document_template_version_template", columnList = "template_id, version"),
                @Index(name = "idx_document_template_version_status", columnList = "com_id, template_id, status, version")
        })
public class DocumentTemplateVersion extends AuditModel {

    @Id @SnowflakeId @Note("模板版本索引")
    private Long versionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @Note("企业模板")
    private DocumentTemplate template;

    @Column(nullable = false) @Note("版本号")
    private Integer version = 1;

    @Column(length = 128, nullable = false) @Note("模板名称")
    private String name;

    @Column(name = "preset_code", length = 64) @Note("来源预设编码")
    private String presetCode;

    @Column(nullable = false) @Note("继承基线版本")
    private Integer baselineVersion = 1;

    @Column(columnDefinition = "LONGTEXT", nullable = false) @Note("字段配置JSON")
    private String fieldConfig;

    @Column(columnDefinition = "LONGTEXT", nullable = false) @Note("受控样式JSON")
    private String styleConfig;

    @Column(name = "layout_config", columnDefinition = "LONGTEXT") @Note("受控版式JSON")
    private String layoutConfig;

    @Column(name = "designer_config", columnDefinition = "LONGTEXT") @Note("凭证设计器JSON")
    private String designerConfig;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("版本状态")
    private DocumentTemplateVersionStatus status = DocumentTemplateVersionStatus.Draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_id")
    @Note("发布人")
    private Employee publishedBy;

    @Column(name = "published_at") @Note("发布时间")
    private LocalDateTime publishedAt;

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public DocumentTemplate getTemplate() { return template; }
    public void setTemplate(DocumentTemplate template) { this.template = template; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPresetCode() { return presetCode; }
    public void setPresetCode(String presetCode) { this.presetCode = presetCode; }
    public Integer getBaselineVersion() { return baselineVersion; }
    public void setBaselineVersion(Integer baselineVersion) { this.baselineVersion = baselineVersion; }
    public String getFieldConfig() { return fieldConfig; }
    public void setFieldConfig(String fieldConfig) { this.fieldConfig = fieldConfig; }
    public String getStyleConfig() { return styleConfig; }
    public void setStyleConfig(String styleConfig) { this.styleConfig = styleConfig; }
    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
    public String getDesignerConfig() { return designerConfig; }
    public void setDesignerConfig(String designerConfig) { this.designerConfig = designerConfig; }
    public DocumentTemplateVersionStatus getStatus() { return status; }
    public void setStatus(DocumentTemplateVersionStatus status) { this.status = status; }
    public Employee getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Employee publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
