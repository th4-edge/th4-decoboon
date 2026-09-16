package com.th4.edge.decoboon.pack.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.model.AbstractModel;


import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.name.annotation.Name;
import jakarta.persistence.*;
import java.util.List;
import com.th4.edge.tenant.user.model.Company;



@Note("装修空间")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "pack_section")
public class PackageSection extends AbstractModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;


    @Note("空间索引")
    @Id
    @SnowflakeId
    private Long groupId;

    @Note("空间名称")
    @Name
    @Keyword
    @Column(length = 64,nullable = false)
    private String name;


    @Note("空间类型")
    @ElementCollection
    @CollectionTable(name = "pack_section_space_types")
    private java.util.List<SpaceType> spaceTypes;
    @Note("可删减数")
    @Description("用于套餐分项删除限制")
    @Column
    private Integer maxDeleteNum=1;

    @Note("优先级")
    @Column
    private Integer priority=0;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SpaceType> getSpaceTypes() {
        return spaceTypes;
    }

    public void setSpaceTypes(List<SpaceType> spaceTypes) {
        this.spaceTypes = spaceTypes;
    }

    public Integer getMaxDeleteNum() {
        return maxDeleteNum;
    }

    public void setMaxDeleteNum(Integer maxDeleteNum) {
        this.maxDeleteNum = maxDeleteNum;
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
