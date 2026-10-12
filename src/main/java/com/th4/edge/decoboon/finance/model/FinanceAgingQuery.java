package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 账龄报表查询条件。 */
@Note("账龄报表查询")
public class FinanceAgingQuery {

    @Note("统计截止日，默认当天")
    private LocalDate asOf;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }

}
