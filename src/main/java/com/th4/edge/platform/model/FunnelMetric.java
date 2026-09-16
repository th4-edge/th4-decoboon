package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 运营漏斗阶段数据。 */
@Note("运营漏斗阶段")
@Model
public class FunnelMetric {
    @Note("阶段名称")
    private String name;
    @Note("阶段数量")
    private Long value;
    @Note("阶段转化率")
    private Double conversionRate;

    public FunnelMetric() {}
    public FunnelMetric(String name, Long value, Double conversionRate) {
        this.name = name;
        this.value = value;
        this.conversionRate = conversionRate;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
}
