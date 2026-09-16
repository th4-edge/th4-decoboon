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
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** 施工项目专项配置。 */
@Note("施工项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_construction_detail")
public class ConstructionItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("施工阶段")
    @Column(length = 64)
    private String stage;

    @Note("计量公式")
    @Column(length = 256)
    private String measurementFormula;

    @Note("施工工艺")
    @Lob
    private String craft;

    @Note("验收标准")
    @Lob
    private String criterion;

    @Note("是否隐蔽工程")
    @Column(nullable = false)
    private Boolean concealed = Boolean.FALSE;

    @Note("默认工期天数")
    @Column(nullable = false)
    private Integer durationDays = 0;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getMeasurementFormula() { return measurementFormula; }
    public void setMeasurementFormula(String measurementFormula) { this.measurementFormula = measurementFormula; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }
    public Boolean getConcealed() { return concealed; }
    public void setConcealed(Boolean concealed) { this.concealed = concealed; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
}
