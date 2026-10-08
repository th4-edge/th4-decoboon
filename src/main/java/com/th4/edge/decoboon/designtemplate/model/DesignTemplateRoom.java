package com.th4.edge.decoboon.designtemplate.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 设计模版区域骨架。
 *
 * <p>尺寸为默认值可留空；公式专业变量：L/W/H/DW/DA/WA/WR/N（BR-040 §2 / §4）。</p>
 */
@Note("设计模版区域")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "design_template_room")
public class DesignTemplateRoom extends AuditModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属模版版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private DesignTemplateVersion version;

    @Note("模版区域")
    @Id
    @SnowflakeId
    private Long roomId;

    @Note("区域唯一键（公式锚点）")
    @Column(name = "room_key", length = 32, nullable = false)
    private String roomKey;

    @Note("区域名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Note("区域类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 32, nullable = false)
    private SpaceType roomType = SpaceType.Bedroom;

    @Note("展示顺序")
    @Column(name = "sort_no", nullable = false)
    private Integer sortNo = 0;

    @Note("开间/长（m）")
    @Column(name = "length_m", precision = 10, scale = 3)
    private BigDecimal length;

    @Note("进深/宽（m）")
    @Column(precision = 10, scale = 3)
    private BigDecimal width;

    @Note("层高（m）")
    @Column(precision = 10, scale = 3)
    private BigDecimal height;

    @Note("门洞总宽（m）")
    @Column(name = "door_width_total", precision = 10, scale = 3)
    private BigDecimal doorWidthTotal;

    @Note("门洞总面积（m²）")
    @Column(name = "door_area_total", precision = 10, scale = 3)
    private BigDecimal doorAreaTotal;

    @Note("窗洞总面积（m²）")
    @Column(name = "window_area_total", precision = 10, scale = 3)
    private BigDecimal windowAreaTotal;

    @Note("上返高度（m）")
    @Column(name = "wall_rise", precision = 10, scale = 3)
    private BigDecimal wallRise;

    @Note("同型区域数量")
    @Column(name = "room_count", nullable = false)
    private Integer count = 1;

    @Note("备注")
    @Column(length = 256)
    private String remark;

    public Company getCompany() { return company; }

    public void setCompany(Company company) { this.company = company; }

    public DesignTemplateVersion getVersion() { return version; }

    public void setVersion(DesignTemplateVersion version) { this.version = version; }

    public Long getRoomId() { return roomId; }

    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomKey() { return roomKey; }

    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public SpaceType getRoomType() { return roomType; }

    public void setRoomType(SpaceType roomType) { this.roomType = roomType; }

    public Integer getSortNo() { return sortNo; }

    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public BigDecimal getLength() { return length; }

    public void setLength(BigDecimal length) { this.length = length; }

    public BigDecimal getWidth() { return width; }

    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }

    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getDoorWidthTotal() { return doorWidthTotal; }

    public void setDoorWidthTotal(BigDecimal doorWidthTotal) { this.doorWidthTotal = doorWidthTotal; }

    public BigDecimal getDoorAreaTotal() { return doorAreaTotal; }

    public void setDoorAreaTotal(BigDecimal doorAreaTotal) { this.doorAreaTotal = doorAreaTotal; }

    public BigDecimal getWindowAreaTotal() { return windowAreaTotal; }

    public void setWindowAreaTotal(BigDecimal windowAreaTotal) { this.windowAreaTotal = windowAreaTotal; }

    public BigDecimal getWallRise() { return wallRise; }

    public void setWallRise(BigDecimal wallRise) { this.wallRise = wallRise; }

    public Integer getCount() { return count; }

    public void setCount(Integer count) { this.count = count; }

    public String getRemark() { return remark; }

    public void setRemark(String remark) { this.remark = remark; }
}
