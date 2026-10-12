package com.th4.edge.decoboon.design.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.Area;
import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 设计方案内的区域尺寸。 */
@Note("方案区域")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_space", indexes = {
        @Index(name = "idx_design_space_company", columnList = "com_id, space_id"),
        @Index(name = "idx_design_space_company_store", columnList = "com_id, store_id, space_id"),
        @Index(name = "idx_design_space_company_team", columnList = "com_id, team_id, space_id"),
        @Index(name = "idx_design_space_company_owner", columnList = "com_id, emp_id, space_id"),
        @Index(name = "idx_design_space_company_design", columnList = "com_id, design_id, space_id")
})
public class Space extends AuditModel implements Area {

    @Note("区域索引")
    @Id
    @SnowflakeId
    private Long spaceId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("所属方案")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("空间类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private SpaceType type = SpaceType.LivingRoom;

    @Note("区域名称")
    @Keyword
    @Column(length = 64, nullable = false)
    private String name;

    @Note("长度")
    @Column(name = "length_m", precision = 10, scale = 3)
    private BigDecimal length;

    @Note("宽度")
    @Column(precision = 10, scale = 3)
    private BigDecimal width;

    @Note("层高")
    @Column(precision = 10, scale = 3)
    private BigDecimal height;

    @Note("实测面积")
    @Column(precision = 12, scale = 2)
    private BigDecimal area;

    @Note("实测周长")
    @Column(precision = 12, scale = 2)
    private BigDecimal perimeter;

    @Note("面积口径来源")
    @Enumerated(EnumType.STRING)
    @Column(name = "area_source", length = 16)
    private AreaSource areaSource;

    @Note("区域值来源：房屋档案继承 / 方案覆盖 / 模版参考值（DEC-064）")
    @Enumerated(EnumType.STRING)
    @Column(name = "value_origin", length = 24)
    private SpaceValueOrigin valueOrigin;

    @Note("来源房屋区域（房屋区域实测档案 regionId；无关联为 null）")
    @Column(name = "house_region_id")
    private Long houseRegionId;

    @Note("最近一次同步时的房屋实测面积（㎡）：用于识别「房屋值已变更」（DEC-064）")
    @Column(name = "inherited_area", precision = 12, scale = 2)
    private BigDecimal inheritedArea;

    @Note("最近一次同步时的房屋实测周长（m）")
    @Column(name = "inherited_perimeter", precision = 12, scale = 2)
    private BigDecimal inheritedPerimeter;

    @Note("覆盖录入人（方案内覆盖房屋实测值时留痕）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_by_id")
    private Employee overrideBy;

    @Note("覆盖录入时间")
    private LocalDateTime overrideAt;

    @Transient
    @Note("只读投影：房屋实测值已变更、需要设计师复核或显式同步")
    private Boolean houseValueStale;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("区域说明")
    @Column(length = 512)
    private String remark;

    @Note("区域图片")
    @ElementCollection
    @CollectionTable(name = "db_design_space_images", joinColumns = @JoinColumn(name = "space_id"))
    @Column(name = "url", length = 512)
    private List<String> images;

    @Override
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    @Override
    public BigDecimal getPerimeter() { return perimeter; }
    public void setPerimeter(BigDecimal perimeter) { this.perimeter = perimeter; }
    public AreaSource getAreaSource() { return areaSource; }
    public void setAreaSource(AreaSource areaSource) { this.areaSource = areaSource; }
    public SpaceValueOrigin getValueOrigin() { return valueOrigin; }
    public void setValueOrigin(SpaceValueOrigin valueOrigin) { this.valueOrigin = valueOrigin; }
    public Long getHouseRegionId() { return houseRegionId; }
    public void setHouseRegionId(Long houseRegionId) { this.houseRegionId = houseRegionId; }
    public BigDecimal getInheritedArea() { return inheritedArea; }
    public void setInheritedArea(BigDecimal inheritedArea) { this.inheritedArea = inheritedArea; }
    public BigDecimal getInheritedPerimeter() { return inheritedPerimeter; }
    public void setInheritedPerimeter(BigDecimal inheritedPerimeter) { this.inheritedPerimeter = inheritedPerimeter; }
    public Employee getOverrideBy() { return overrideBy; }
    public void setOverrideBy(Employee overrideBy) { this.overrideBy = overrideBy; }
    public LocalDateTime getOverrideAt() { return overrideAt; }
    public void setOverrideAt(LocalDateTime overrideAt) { this.overrideAt = overrideAt; }
    public Boolean getHouseValueStale() { return houseValueStale; }
    public void setHouseValueStale(Boolean houseValueStale) { this.houseValueStale = houseValueStale; }
    public Long getSpaceId() { return spaceId; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Design getDesign() { return design; }
    public void setDesign(Design design) { this.design = design; }
    public SpaceType getType() { return type; }
    public void setType(SpaceType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getLength() { return length; }
    public void setLength(BigDecimal length) { this.length = length; }
    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
