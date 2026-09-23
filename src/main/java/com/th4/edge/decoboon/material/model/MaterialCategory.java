package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 企业主材分类。
 *
 * <p>DEC-MP-09：损耗率在"主材分类"上设置（如分类"瓷砖"），材料计划行按其主材分类继承；
 * 分类未设置时损耗率为 0，产品不预置行业比例。纯企业级主数据，不套用门店/团队数据范围。</p>
 */
@Note("主材分类")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_material_category",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_category_company_name",
                columnNames = {"com_id", "name"}),
        indexes = {
                @Index(name = "idx_material_category_company", columnList = "com_id, material_category_id"),
                @Index(name = "idx_material_category_company_name", columnList = "com_id, name")
        })
public class MaterialCategory extends AuditModel {

    @Id
    @SnowflakeId
    @Note("主材分类索引")
    private Long materialCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Keyword
    @Column(length = 64, nullable = false)
    @Note("分类名称（如瓷砖、地板、木门、五金、涂料）")
    private String name;

    @Column(name = "default_loss_rate", precision = 8, scale = 4, nullable = false)
    @Note("默认损耗率（百分比，默认 0，由企业自行设定）")
    private BigDecimal defaultLossRate = BigDecimal.ZERO;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    @Column(nullable = false)
    @Note("是否启用")
    private Boolean active = Boolean.TRUE;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @Note("创建人")
    private Employee createdBy;

    public Long getMaterialCategoryId() { return materialCategoryId; }
    public void setMaterialCategoryId(Long materialCategoryId) { this.materialCategoryId = materialCategoryId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDefaultLossRate() { return defaultLossRate; }
    public void setDefaultLossRate(BigDecimal defaultLossRate) { this.defaultLossRate = defaultLossRate; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
