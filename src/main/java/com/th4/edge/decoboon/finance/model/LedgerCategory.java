package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/** 企业经营业务分类，两级结构；内置分类按企业首次使用时生成，企业可追加自定义分类。 */
@Note("手工记账业务分类")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_ledger_category",
        uniqueConstraints = @UniqueConstraint(name = "uk_fin_category_company_code",
                columnNames = {"com_id", "direction", "code"}),
        indexes = {
                @Index(name = "idx_fin_category_company_parent", columnList = "com_id, parent_id, direction, sort_no"),
                @Index(name = "idx_fin_category_company", columnList = "com_id, category_id")
        })
public class LedgerCategory extends AuditModel {

    @Id
    @SnowflakeId
    @Note("分类索引")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Note("上级分类")
    private LedgerCategory parent;

    @Note("分类名称")
    @Keyword
    @Column(length = 64, nullable = false)
    private String name;

    @Note("收支方向")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerDirection direction;

    @Note("内置稳定编码；企业自定义分类为空")
    @Column(length = 64)
    private String code;

    @Note("是否内置分类")
    @Column(name = "built_in", nullable = false)
    private Boolean builtIn = Boolean.FALSE;

    @Note("显示排序")
    @Column(name = "sort_no", nullable = false)
    private Integer sort = 0;

    @Note("是否启用")
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @Note("创建人")
    private Employee createdBy;

    @Transient
    @Note("子分类，仅树形返回时使用")
    private List<LedgerCategory> children = new ArrayList<>();

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public LedgerCategory getParent() { return parent; }
    public void setParent(LedgerCategory parent) { this.parent = parent; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LedgerDirection getDirection() { return direction; }
    public void setDirection(LedgerDirection direction) { this.direction = direction; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public List<LedgerCategory> getChildren() { return children; }
    public void setChildren(List<LedgerCategory> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }
}
