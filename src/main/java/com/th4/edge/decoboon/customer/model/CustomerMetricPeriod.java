package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 客户经营指标卡的日期窗口切片。 */
@Note("客户经营指标日期窗口")
public class CustomerMetricPeriod {

    private String key;
    private String label;
    private BigDecimal value = BigDecimal.ZERO;
    private String valueType = "count";

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
}
