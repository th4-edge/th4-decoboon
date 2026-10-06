package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/** 企业单据模板；平台行业基线以瞬态副本返回。 */
@Note("企业单据模板")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_document_template", indexes = {
        @Index(name = "idx_document_template_company", columnList = "com_id, template_id"),
        @Index(name = "idx_document_template_company_type", columnList = "com_id, type, version"),
        @Index(name = "idx_document_template_company_preset", columnList = "com_id, type, preset_code, version")
})
public class DocumentTemplate extends AuditModel {

    @Id @SnowflakeId @Note("模板索引") private Long templateId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) @Note("单据类型") private DocumentType type = DocumentType.Quote;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("模板范围") private DocumentTemplateScope scope = DocumentTemplateScope.Company;
    @Column(length = 128, nullable = false) @Note("模板名称") private String name;
    @Column(nullable = false) @Note("模板版本") private Integer version = 1;
    @Column(nullable = false) @Note("继承基线版本") private Integer baselineVersion = 1;
    @Column(columnDefinition = "LONGTEXT", nullable = false) @Note("字段配置JSON") private String fieldConfig;
    @Column(columnDefinition = "LONGTEXT", nullable = false) @Note("受控样式JSON") private String styleConfig;
    @Column(name = "layout_config", columnDefinition = "LONGTEXT") @Note("受控版式JSON") private String layoutConfig;
    @Column(name = "designer_config", columnDefinition = "LONGTEXT") @Note("凭证设计器JSON") private String designerConfig;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("模板状态") private DocumentStatus status = DocumentStatus.Active;

    @Column(name = "preset_code", length = 64) @Note("来源预设编码")
    private String presetCode;

    @Transient @Note("预设适用说明（仅平台预设返回）")
    private String presetNote;

    @Column(name = "published_version", nullable = false) @Note("当前发布版本")
    private Integer publishedVersion = 0;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "published_by_id") @Note("当前版本发布人")
    private Employee publishedBy;

    @Column(name = "published_at") @Note("当前版本发布时间")
    private LocalDateTime publishedAt;

    @Transient @Note("模板版本历史")
    private List<DocumentTemplateVersion> versions = List.of();

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public DocumentTemplateScope getScope() { return scope; }
    public void setScope(DocumentTemplateScope scope) { this.scope = scope; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
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
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getPresetCode() { return presetCode; }
    public void setPresetCode(String presetCode) { this.presetCode = presetCode; }
    public String getPresetNote() { return presetNote; }
    public void setPresetNote(String presetNote) { this.presetNote = presetNote; }
    public Integer getPublishedVersion() { return publishedVersion; }
    public void setPublishedVersion(Integer publishedVersion) { this.publishedVersion = publishedVersion; }
    public Employee getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Employee publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public List<DocumentTemplateVersion> getVersions() { return versions; }
    public void setVersions(List<DocumentTemplateVersion> versions) { this.versions = versions == null ? List.of() : List.copyOf(versions); }
}
