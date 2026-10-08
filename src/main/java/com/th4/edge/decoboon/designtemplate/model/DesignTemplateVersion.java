package com.th4.edge.decoboon.designtemplate.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计模版版本（内容快照）。
 *
 * <p>名称、房型、适用面积、标签、区域与分项共同构成版本内容；版本一经发布
 * **不可修改**，修改模版即新建版本（BR-040 §5）。因此模版主档不再持有这些
 * 内容字段，避免已发布版本被后续编辑改写。</p>
 */
@Note("设计模版版本")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "design_template_version")
public class DesignTemplateVersion extends AuditModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属模版")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DesignTemplate template;

    @Note("模版版本")
    @Id
    @SnowflakeId
    private Long versionId;

    @Note("版本号")
    @Column(nullable = false)
    private Integer version = 0;

    @Note("状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private DesignTemplate.Status status = DesignTemplate.Status.Draft;

    @Note("模版名称")
    @Keyword
    @Column(length = 64, nullable = false)
    private String name;

    @Note("房型标签")
    @Keyword
    @Column(name = "house_type", length = 64, nullable = false)
    private String houseType;

    @Note("适用套内面积下限")
    @Column(name = "area_min", precision = 12, scale = 2)
    private BigDecimal areaMin;

    @Note("适用套内面积上限")
    @Column(name = "area_max", precision = 12, scale = 2)
    private BigDecimal areaMax;

    @Note("风格/用途标签")
    @Column(length = 256)
    private String tags;

    @Note("发布时间")
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Note("发布人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private Employee publishedBy;

    public Company getCompany() { return company; }

    public void setCompany(Company company) { this.company = company; }

    public DesignTemplate getTemplate() { return template; }

    public void setTemplate(DesignTemplate template) { this.template = template; }

    public Long getVersionId() { return versionId; }

    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public Integer getVersion() { return version; }

    public void setVersion(Integer version) { this.version = version; }

    public DesignTemplate.Status getStatus() { return status; }

    public void setStatus(DesignTemplate.Status status) { this.status = status; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getHouseType() { return houseType; }

    public void setHouseType(String houseType) { this.houseType = houseType; }

    public BigDecimal getAreaMin() { return areaMin; }

    public void setAreaMin(BigDecimal areaMin) { this.areaMin = areaMin; }

    public BigDecimal getAreaMax() { return areaMax; }

    public void setAreaMax(BigDecimal areaMax) { this.areaMax = areaMax; }

    public String getTags() { return tags; }

    public void setTags(String tags) { this.tags = tags; }

    public LocalDateTime getPublishedAt() { return publishedAt; }

    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public Employee getPublishedBy() { return publishedBy; }

    public void setPublishedBy(Employee publishedBy) { this.publishedBy = publishedBy; }
}
