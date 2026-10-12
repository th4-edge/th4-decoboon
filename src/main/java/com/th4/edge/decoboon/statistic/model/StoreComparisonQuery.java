package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 门店对比矩阵查询（`P0D-05`）：期间 = 月（默认当月）。 */
@Note("门店对比查询")
public class StoreComparisonQuery {

    @Note("期间（月，默认当月）")
    private LocalDate month;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
}
