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

import java.math.BigDecimal;

/** 主材项目专项配置。 */
@Note("主材项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_material_detail")
public class MaterialItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("型号")
    @Column(length = 128)
    private String model;

    @Note("颜色")
    @Column(length = 64)
    private String color;

    @Note("材质")
    @Column(length = 128)
    private String material;

    @Note("损耗率")
    @Column(precision = 10, scale = 4, nullable = false)
    private BigDecimal lossRate = BigDecimal.ZERO;

    @Note("最小起订量")
    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal minimumOrder = BigDecimal.ONE;

    @Note("库存方式")
    @Column(length = 32)
    private String inventoryMode;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(BigDecimal minimumOrder) { this.minimumOrder = minimumOrder; }
    public String getInventoryMode() { return inventoryMode; }
    public void setInventoryMode(String inventoryMode) { this.inventoryMode = inventoryMode; }
}
