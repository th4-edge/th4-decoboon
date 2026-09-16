package com.th4.edge.decoboon.pack.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;

/** 套餐模板中的标准项目行。 */
@Note("套餐模板项目行")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "pack_line")
public class PackageLine extends AbstractModel {

    @Note("分项索引")
    @Id
    @SnowflakeId
    private Long itemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属套餐模板")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PackageTemplate template;

    @Note("所属套餐分组")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private PackageSection section;

    @Note("企业报价项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItem item;

    @Note("套餐内名称")
    @Keyword
    @Column(length = 128, nullable = false)
    private String name;

    @Note("品牌")
    @Column(length = 128)
    private String brand;

    @Note("规格")
    @Column(length = 128)
    private String spec;

    @Note("单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Unit unit = Unit.Item;

    @Note("套餐包含数量或数量公式")
    @Column(length = 256)
    private String quantity;

    @Note("数量换算系数")
    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal quantityFactor = BigDecimal.ONE;

    @Note("套餐计价方式")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private PricingMethod pricingMethod = PricingMethod.MaterialAndManual;

    @Note("是否默认包含")
    @Column(nullable = false)
    private Boolean included = Boolean.TRUE;

    @Note("是否允许扣减")
    @Column(nullable = false)
    private Boolean deductible = Boolean.TRUE;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("套餐项目说明")
    @ElementCollection
    @CollectionTable(name = "pack_line_criterions", joinColumns = @JoinColumn(name = "item_id"))
    @Column(length = 256)
    private List<String> criterions;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public PackageTemplate getTemplate() { return template; }
    public void setTemplate(PackageTemplate template) { this.template = template; }
    public PackageSection getSection() { return section; }
    public void setSection(PackageSection section) { this.section = section; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public BigDecimal getQuantityFactor() { return quantityFactor; }
    public void setQuantityFactor(BigDecimal quantityFactor) { this.quantityFactor = quantityFactor; }
    public PricingMethod getPricingMethod() { return pricingMethod; }
    public void setPricingMethod(PricingMethod pricingMethod) { this.pricingMethod = pricingMethod; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
    public Boolean getDeductible() { return deductible; }
    public void setDeductible(Boolean deductible) { this.deductible = deductible; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public List<String> getCriterions() { return criterions; }
    public void setCriterions(List<String> criterions) { this.criterions = criterions; }
}
