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
import java.util.List;
import java.util.Map;
import com.th4.edge.tenant.user.model.Company;




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

    @Note("套餐价格")
    @Unit("元")
    @Description("折扣后一口价")
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



    public PackageTemplate() {
        this.area=new java.math.BigDecimal(80);
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

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
}
