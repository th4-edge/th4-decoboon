package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.TagCategory;
import com.th4.edge.decoboon.customer.configure.TagScope;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
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

/**
 * 客户标签字典（企业级）。
 *
 * <p>标签字典仅企业负责人和店长可维护；普通业务员与设计师只能选用。
 * {@link TagScope#System} 的字典项由系统事实派生，任何角色均不可改名或删除。</p>
 */
@Note("客户标签字典")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_tag", indexes = {
        @Index(name = "idx_cust_tag_company", columnList = "com_id, tag_id"),
        @Index(name = "idx_cust_tag_company_category", columnList = "com_id, category, status"),
        @Index(name = "idx_cust_tag_company_name", columnList = "com_id, name", unique = true)
})
public class CustomerTag extends AuditModel {

    @Id
    @SnowflakeId
    @Note("标签索引")
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Keyword
    @Column(length = 20, nullable = false)
    @Note("标签名称")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("标签分类")
    private TagCategory category = TagCategory.Intent;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("标签来源：系统自动或人工维护")
    private TagScope scope = TagScope.Manual;

    @Column(length = 16)
    @Note("标签颜色")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("标签状态")
    private TagStatus status = TagStatus.Enabled;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    @Transient
    @Note("标签业务键（系统标签用于前端筛选与扣分判定）")
    private String key;

    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TagCategory getCategory() { return category; }
    public void setCategory(TagCategory category) { this.category = category; }
    public TagScope getScope() { return scope; }
    public void setScope(TagScope scope) { this.scope = scope; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public TagStatus getStatus() { return status; }
    public void setStatus(TagStatus status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
