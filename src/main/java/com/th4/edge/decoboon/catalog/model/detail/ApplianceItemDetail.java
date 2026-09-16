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

/** 电器设备项目专项配置。 */
@Note("电器设备项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_appliance_detail")
public class ApplianceItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("型号")
    @Column(length = 128)
    private String model;

    @Note("功率")
    @Column(length = 64)
    private String power;

    @Note("设备尺寸")
    @Column(length = 128)
    private String dimensions;

    @Note("水电安装条件")
    @Column(length = 512)
    private String utilityRequirement;

    @Note("保修月数")
    @Column(nullable = false)
    private Integer warrantyMonths = 0;

    @Note("售后服务说明")
    @Column(length = 512)
    private String afterSales;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPower() { return power; }
    public void setPower(String power) { this.power = power; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getUtilityRequirement() { return utilityRequirement; }
    public void setUtilityRequirement(String utilityRequirement) { this.utilityRequirement = utilityRequirement; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getAfterSales() { return afterSales; }
    public void setAfterSales(String afterSales) { this.afterSales = afterSales; }
}
