package com.th4.edge.decoboon.property.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋区域实测档案（区域实测面积/周长的**唯一事实源**）。
 *
 * <p>{@code DEC-064} / {@code OBJ-007} / {@code EV-222}：同一房屋的区域实测值只需录入一次，
 * 该房屋的多个设计方案实例化时按 {@code roomKey} → 「区域类型 + 标准化名称」匹配继承，
 * 免去重复录入；修改本档案的实测值时，引用它的**草稿态**方案自动重算，
 * 非草稿方案只提示复核（不静默改金额）。</p>
 *
 * <p>面积/周长可空表示未录入，不得用 0 表达未录入；{@code areaSource} 只有 {@code Measured}
 * 才能作为工程量基数（{@code DEC-062}）。</p>
 */
@Note("房屋区域实测档案")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_property_region", indexes = {
        @Index(name = "idx_property_region_company", columnList = "com_id, region_id"),
        @Index(name = "idx_property_region_company_property", columnList = "com_id, property_id, region_id"),
        @Index(name = "idx_property_region_company_store", columnList = "com_id, store_id, region_id"),
        @Index(name = "idx_property_region_company_team", columnList = "com_id, team_id, region_id"),
        @Index(name = "idx_property_region_company_owner", columnList = "com_id, emp_id, region_id")
})
public class PropertyRegion extends AuditModel {

    @Note("房屋区域")
    @Id
    @SnowflakeId
    private Long regionId;

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

    @Note("数据负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("所属房屋")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Note("区域键（与模版区域对齐，可空）")
    @Column(name = "room_key", length = 32)
    private String roomKey;

    @Note("区域类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 32, nullable = false)
    private SpaceType roomType = SpaceType.Bedroom;

    @Note("区域名称")
    @Keyword
    @Column(length = 64, nullable = false)
    private String name;

    @Note("量房快照 开间/长（m，非工程量依据）")
    @Column(name = "length_m", precision = 10, scale = 3)
    private BigDecimal length;

    @Note("量房快照 进深/宽（m，非工程量依据）")
    @Column(precision = 10, scale = 3)
    private BigDecimal width;

    @Note("层高（m）")
    @Column(precision = 10, scale = 3)
    private BigDecimal height;

    @Note("实测面积（㎡）：设计师按 CAD 录入，区域分项工程量基数")
    @Column(precision = 12, scale = 2)
    private BigDecimal area;

    @Note("实测周长（m）：设计师按 CAD 录入")
    @Column(precision = 12, scale = 2)
    private BigDecimal perimeter;

    @Note("面积口径来源：只有 Measured 可作工程量基数（DEC-062）")
    @Enumerated(EnumType.STRING)
    @Column(name = "area_source", length = 16)
    private AreaSource areaSource;

    @Note("实测录入人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measured_by_id")
    private Employee measuredBy;

    @Note("实测录入时间")
    private LocalDateTime measuredAt;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("备注")
    @Column(length = 512)
    private String remark;

    @Transient
    @Note("只读投影：本次保存联动重算的草稿方案数")
    private Integer linkedDesignCount;

    @Transient
    @Note("只读投影：引用该区域但未自动重算的方案数（非草稿，需显式同步）")
    private Integer pendingDesignCount;

    public Long getRegionId() { return regionId; }

    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public Company getCompany() { return company; }

    public void setCompany(Company company) { this.company = company; }

    public Store getStore() { return store; }

    public void setStore(Store store) { this.store = store; }

    public Team getTeam() { return team; }

    public void setTeam(Team team) { this.team = team; }

    public Employee getOwner() { return owner; }

    public void setOwner(Employee owner) { this.owner = owner; }

    public Property getProperty() { return property; }

    public void setProperty(Property property) { this.property = property; }

    public String getRoomKey() { return roomKey; }

    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }

    public SpaceType getRoomType() { return roomType; }

    public void setRoomType(SpaceType roomType) { this.roomType = roomType; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public BigDecimal getLength() { return length; }

    public void setLength(BigDecimal length) { this.length = length; }

    public BigDecimal getWidth() { return width; }

    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }

    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getArea() { return area; }

    public void setArea(BigDecimal area) { this.area = area; }

    public BigDecimal getPerimeter() { return perimeter; }

    public void setPerimeter(BigDecimal perimeter) { this.perimeter = perimeter; }

    public AreaSource getAreaSource() { return areaSource; }

    public void setAreaSource(AreaSource areaSource) { this.areaSource = areaSource; }

    public Employee getMeasuredBy() { return measuredBy; }

    public void setMeasuredBy(Employee measuredBy) { this.measuredBy = measuredBy; }

    public LocalDateTime getMeasuredAt() { return measuredAt; }

    public void setMeasuredAt(LocalDateTime measuredAt) { this.measuredAt = measuredAt; }

    public Integer getSort() { return sort; }

    public void setSort(Integer sort) { this.sort = sort; }

    public String getRemark() { return remark; }

    public void setRemark(String remark) { this.remark = remark; }

    public Integer getLinkedDesignCount() { return linkedDesignCount; }

    public void setLinkedDesignCount(Integer linkedDesignCount) { this.linkedDesignCount = linkedDesignCount; }

    public Integer getPendingDesignCount() { return pendingDesignCount; }

    public void setPendingDesignCount(Integer pendingDesignCount) { this.pendingDesignCount = pendingDesignCount; }
}
