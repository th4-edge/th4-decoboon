package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 企业负责人项目经营指标卡。 */
@Note("项目经营指标卡")
public class ProjectMetricCard {

    private String key;
    private String label;
    private BigDecimal value = BigDecimal.ZERO;
    private String valueType = "count";
    private String status = "normal";
    private String subText;
    private BigDecimal trendRate;

    public ProjectMetricCard() { }

    public ProjectMetricCard(String key, String label, BigDecimal value, String valueType,
                             String status, String subText) {
        this.key = key;
        this.label = label;
        this.value = value == null ? BigDecimal.ZERO : value;
        this.valueType = valueType;
        this.status = status;
        this.subText = subText;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubText() { return subText; }
    public void setSubText(String subText) { this.subText = subText; }
    public BigDecimal getTrendRate() { return trendRate; }
    public void setTrendRate(BigDecimal trendRate) { this.trendRate = trendRate; }
}
