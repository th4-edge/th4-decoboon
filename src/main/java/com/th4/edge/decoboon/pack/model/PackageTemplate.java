package com.th4.edge.decoboon.pack.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;


import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.MapNote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.IgnoreProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.ServiceProperties;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.name.annotation.Name;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;




@Note("装修套餐")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "pack_template")
public class PackageTemplate extends AbstractModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;


    @Note("套餐索引")
    @Id
    @SnowflakeId
    private Long templateId;

    @Note("套餐编码")
    @Keyword
    @Column(length = 64, nullable = false)
    private String code;

    @Note("套餐版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("套餐状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PackageTemplateStatus status = PackageTemplateStatus.Draft;

    @Note("是否当前发布版本")
    @Column(name = "is_current_published", nullable = false)
    private Boolean currentPublished = Boolean.FALSE;

    @Note("发布时间")
    private LocalDateTime publishedAt;

    @Note("发布人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_id")
    private Employee publishedBy;

    @Note("发布快照")
    @Lob
    @Column(name = "snapshot_json", columnDefinition = "longtext")
    private String snapshotJson;

    @Note("发布快照哈希")
    @Column(name = "snapshot_hash", length = 64)
    private String snapshotHash;


    @Note("套餐名称")
    @Name
    @Keyword
    @Column(length = 128,nullable = false)
    private String name;



    @Note("套餐面积")
    @Unit("m²")
    @Description("用于计算套餐价格的基础面积")
    @Column(nullable = false)
    private java.math.BigDecimal area;

    @Note("套餐牌价")
    @Unit("元")
    @Description("套餐标准面积对应的折前牌价；实际报价金额再应用 discount")
    @Column(nullable = false,precision = 10,scale = 2)
    private java.math.BigDecimal price;

    @Note("折扣率")
    @Column(nullable = false,precision = 10,scale = 2)
    private java.math.BigDecimal discount= BigDecimal.ONE;

    @Note("成本估算")
    @LogicField
    @Column(nullable = false,precision = 10,scale = 2)
    private java.math.BigDecimal cost=BigDecimal.ZERO;


    @Note("套餐分项")
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PackageLine> items;

    @Note("套餐差项")
    @MapNote(key = "计价差项",value = "计价差值")
    @ElementCollection
    @MapKeyJoinColumn(name = "item_id")
    @CollectionTable(name = "pack_template_measures", joinColumns = @JoinColumn(name = "package_template_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "value.price", column = @Column(name = "price")),
            @AttributeOverride(name = "value.standard", column = @Column(name = "standard")),
            @AttributeOverride(name = "value.remark", column = @Column(name = "remark"))
    })
    private java.util.Map<PackageTemplateItem,PackageRule> measures;

    @Note("套餐描述")
    @ElementCollection
    @CollectionTable(name = "pack_template_criterions", joinColumns = @JoinColumn(name = "package_template_id"))
    @Column(length = 256)
    private java.util.List<String> criterions;

    @Transient @Note("选择器摘要：分项数") private Integer itemCount;
    @Transient @Note("选择器摘要：分区数") private Integer sectionCount;



    public PackageTemplate() {
        this.area=new java.math.BigDecimal(80);
    }

    /** 兼容非生命周期 Provider 的实体保存；正式 Provider 会显式生成编码和版本。 */
    @PrePersist
    private void ensureVersionIdentity() {
        if (code == null || code.isBlank()) {
            code = "PACK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (version == null) version = 1;
        if (status == null) status = PackageTemplateStatus.Draft;
        if (currentPublished == null) currentPublished = Boolean.FALSE;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public PackageTemplateStatus getStatus() { return status; }
    public void setStatus(PackageTemplateStatus status) { this.status = status; }
    public Boolean getCurrentPublished() { return currentPublished; }
    public void setCurrentPublished(Boolean currentPublished) { this.currentPublished = currentPublished; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Employee getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Employee publishedBy) { this.publishedBy = publishedBy; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getSnapshotHash() { return snapshotHash; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<PackageLine> getItems() {
        return items;
    }

    public void setItems(List<PackageLine> items) {
        this.items = items;
    }

    public Map<PackageTemplateItem, PackageRule> getMeasures() {
        return measures;
    }

    public void setMeasures(Map<PackageTemplateItem, PackageRule> measures) {
        this.measures = measures;
    }

    public List<String> getCriterions() {
        return criterions;
    }

    public void setCriterions(List<String> criterions) {
        this.criterions = criterions;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getSectionCount() { return sectionCount; }
    public void setSectionCount(Integer sectionCount) { this.sectionCount = sectionCount; }
}
