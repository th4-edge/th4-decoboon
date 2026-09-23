package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.List;

/** 企业负责人客户经营指标卡。 */
@Note("客户经营指标卡")
public class CustomerMetricCard {
    private String key;
    private String label;
    private BigDecimal value = BigDecimal.ZERO;
    private String valueType = "count";
    private String status = "normal";
    private String subText;
    private BigDecimal trendRate;
    private List<CustomerMetricPeriod> periods = List.of();

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
    public List<CustomerMetricPeriod> getPeriods() { return periods; }
    public void setPeriods(List<CustomerMetricPeriod> periods) {
        this.periods = periods == null ? List.of() : List.copyOf(periods);
    }
}
