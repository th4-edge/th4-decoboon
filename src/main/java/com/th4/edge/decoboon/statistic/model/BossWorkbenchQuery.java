package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 企业负责人首页查询。 */
@Note("企业负责人首页查询")
public class BossWorkbenchQuery {

    private LocalDate month;
    private Integer eventLimit = 6;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Integer getEventLimit() { return eventLimit; }
    public void setEventLimit(Integer eventLimit) { this.eventLimit = eventLimit; }
}
