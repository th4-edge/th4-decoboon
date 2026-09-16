package com.th4.edge.decoboon.pack.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.name.annotation.Name;
import jakarta.persistence.*;
import com.th4.edge.tenant.user.model.Company;


@Note("套餐差项")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "pack_template_item")
public class PackageTemplateItem extends AbstractModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;


    @Note("标准索引")
    @Id
    @SnowflakeId
    private Long itemId;


    @Note("项目名称")
    @Name
    @Keyword
    @Column(length = 128,nullable = false)
    private String name;

    @Note("计价单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32,nullable = false)
    private Unit unit=Unit.Area;


    @Note("计价空间类型")
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @JoinTable(name = "pack_template_item_space_types", joinColumns = @JoinColumn(name = "item_id"))
    @Column(length = 32)
    private java.util.List<SpaceType> spaceTypes;

    @Note("优先级")
    @Column
    private Integer priority=0;


    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public java.util.List<SpaceType> getSpaceTypes() {
        return spaceTypes;
    }

    public void setSpaceTypes(java.util.List<SpaceType> spaceTypes) {
        this.spaceTypes = spaceTypes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
