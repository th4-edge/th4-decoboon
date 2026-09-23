package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 风险中心查询条件。 */
@Note("风险中心查询")
public class RiskCenterQuery {

    @Note("统计截止日，默认当天")
    private LocalDate asOf;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
}
