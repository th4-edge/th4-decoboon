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

/** 全屋定制项目专项配置。 */
@Note("全屋定制项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_custom_detail")
public class CustomFurnitureItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("柜体类型")
    @Column(length = 64)
    private String cabinetType;

    @Note("板材")
    @Column(length = 128)
    private String board;

    @Note("门板")
    @Column(length = 128)
    private String door;

    @Note("台面")
    @Column(length = 128)
    private String countertop;

    @Note("五金")
    @Column(length = 256)
    private String hardware;

    @Note("参数化计价公式")
    @Column(length = 512)
    private String parametricFormula;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getCabinetType() { return cabinetType; }
    public void setCabinetType(String cabinetType) { this.cabinetType = cabinetType; }
    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }
    public String getDoor() { return door; }
    public void setDoor(String door) { this.door = door; }
    public String getCountertop() { return countertop; }
    public void setCountertop(String countertop) { this.countertop = countertop; }
    public String getHardware() { return hardware; }
    public void setHardware(String hardware) { this.hardware = hardware; }
    public String getParametricFormula() { return parametricFormula; }
    public void setParametricFormula(String parametricFormula) { this.parametricFormula = parametricFormula; }
}
