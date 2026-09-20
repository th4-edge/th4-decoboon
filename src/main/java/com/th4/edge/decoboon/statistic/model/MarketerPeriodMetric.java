package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

@Note("业务员周期指标")
public class MarketerPeriodMetric {
    private BigDecimal current = BigDecimal.ZERO;
    private BigDecimal previous = BigDecimal.ZERO;
    private BigDecimal growthRate = BigDecimal.ZERO;

    public MarketerPeriodMetric() { }
    public MarketerPeriodMetric(BigDecimal current, BigDecimal previous, BigDecimal growthRate) {
        this.current = current;
        this.previous = previous;
        this.growthRate = growthRate;
    }

    public BigDecimal getCurrent() { return current; }
    public void setCurrent(BigDecimal current) { this.current = current; }
    public BigDecimal getPrevious() { return previous; }
    public void setPrevious(BigDecimal previous) { this.previous = previous; }
    public BigDecimal getGrowthRate() { return growthRate; }
    public void setGrowthRate(BigDecimal growthRate) { this.growthRate = growthRate; }
}
