package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
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

/** 内容模板。 */
@Note("内容模板")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_template", indexes = {
        @Index(name = "idx_content_template_company", columnList = "com_id, template_id")
})
public class ContentTemplate extends AuditModel {

    @Id @SnowflakeId @Note("模板索引")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id")
    @Note("所属企业，空表示系统模板")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @Column(length = 128, nullable = false)
    @Note("模板名称")
    private String name;

    @Column(length = 16, nullable = false)
    @Note("画幅比例")
    private String format = "9:16";

    @Column(name = "duration_sec", nullable = false)
    @Note("建议时长秒")
    private Integer durationSec = 35;

    @Column(name = "clip_min", nullable = false)
    @Note("最少片段数")
    private Integer clipMin = 3;

    @Column(name = "clip_max", nullable = false)
    @Note("最多片段数")
    private Integer clipMax = 8;

    @Column(name = "script_schema", columnDefinition = "TEXT")
    @Note("脚本结构")
    private String scriptSchema;

    @Column(name = "cover_schema", columnDefinition = "TEXT")
    @Note("封面结构")
    private String coverSchema;

    @Column(name = "allowed_asset_types", length = 255)
    @Note("允许的素材类型")
    private String allowedAssetTypes = "Image,Video";

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("模板状态")
    private ContentAssetStatus status = ContentAssetStatus.InternalReady;

    @Column(nullable = false)
    @Note("版本")
    private Integer version = 1;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public Integer getClipMin() { return clipMin; }
    public void setClipMin(Integer clipMin) { this.clipMin = clipMin; }
    public Integer getClipMax() { return clipMax; }
    public void setClipMax(Integer clipMax) { this.clipMax = clipMax; }
    public String getScriptSchema() { return scriptSchema; }
    public void setScriptSchema(String scriptSchema) { this.scriptSchema = scriptSchema; }
    public String getCoverSchema() { return coverSchema; }
    public void setCoverSchema(String coverSchema) { this.coverSchema = coverSchema; }
    public String getAllowedAssetTypes() { return allowedAssetTypes; }
    public void setAllowedAssetTypes(String allowedAssetTypes) { this.allowedAssetTypes = allowedAssetTypes; }
    public ContentAssetStatus getStatus() { return status; }
    public void setStatus(ContentAssetStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
