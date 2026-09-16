package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 经营预警查询条件。 */
@Note("经营预警查询")
public class BusinessAlertQuery {

    @Note("预警观察日期，默认当天")
    private LocalDate asOf;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
}
