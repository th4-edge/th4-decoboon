package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

@Note("业务员转化率指标")
public class MarketerRateMetric {
    private BigDecimal currentRate = BigDecimal.ZERO;
    private BigDecimal previousRate = BigDecimal.ZERO;
    private BigDecimal change = BigDecimal.ZERO;

    public MarketerRateMetric() { }
    public MarketerRateMetric(BigDecimal currentRate, BigDecimal previousRate, BigDecimal change) {
        this.currentRate = currentRate;
        this.previousRate = previousRate;
        this.change = change;
    }

    public BigDecimal getCurrentRate() { return currentRate; }
    public void setCurrentRate(BigDecimal currentRate) { this.currentRate = currentRate; }
    public BigDecimal getPreviousRate() { return previousRate; }
    public void setPreviousRate(BigDecimal previousRate) { this.previousRate = previousRate; }
    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }
}
