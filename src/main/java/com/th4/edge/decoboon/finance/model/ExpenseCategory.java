package com.th4.edge.decoboon.finance.model;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 企业费用分类；与手工记账 LedgerCategory 分离。 */
@Note("企业费用分类")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_finance_expense_category",
        uniqueConstraints = @UniqueConstraint(name = "uk_fin_expense_category_company_code",
                columnNames = {"com_id", "code"}),
        indexes = {
                @Index(name = "idx_fin_expense_category_company", columnList = "com_id, category_id"),
                @Index(name = "idx_fin_expense_category_sort", columnList = "com_id, active, sort_no")
        })
public class ExpenseCategory extends AuditModel {

    @Id
    @SnowflakeId
    @Note("费用分类索引")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Keyword
    @Column(length = 64, nullable = false)
    @Note("分类编码")
    private String code;

    @Keyword
    @Column(length = 64, nullable = false)
    @Note("分类名称")
    private String name;

    @Column(length = 500)
    @Note("分类说明")
    private String description;

    @Column(name = "built_in", nullable = false)
    @Note("是否平台基础分类")
    private Boolean builtIn = Boolean.FALSE;

    @Column(nullable = false)
    @Note("是否启用")
    private Boolean active = Boolean.TRUE;

    @Column(name = "sort_no", nullable = false)
    @Note("显示排序")
    private Integer sort = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @Note("创建人")
    private Employee createdBy;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
