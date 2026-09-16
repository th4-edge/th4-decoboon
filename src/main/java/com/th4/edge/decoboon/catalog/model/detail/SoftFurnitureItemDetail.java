package com.th4.edge.decoboon.catalog.model.detail;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 软装家具项目专项配置。 */
@Note("软装家具项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_soft_detail")
public class SoftFurnitureItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("风格")
    @Column(length = 64)
    private String style;

    @Note("材质")
    @Column(length = 128)
    private String material;

    @Note("尺寸")
    @Column(length = 128)
    private String dimensions;

    @Note("组合说明")
    @Column(length = 512)
    private String composition;

    @Note("安装方式")
    @Column(length = 64)
    private String installationMode;

    @Note("交付周期天数")
    @Column(nullable = false)
    private Integer leadTimeDays = 0;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getInstallationMode() { return installationMode; }
    public void setInstallationMode(String installationMode) { this.installationMode = installationMode; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
}
