package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 企业负责人驾驶舱查询条件。 */
@Note("企业负责人驾驶舱查询")
public class BossDashboardQuery {

    @Note("统计月份，任意一天代表所在月，默认当前月")
    private LocalDate month;

    @Note("门店范围；为空表示企业全部门店（仅企业负责人可跨门店）")
    private Long storeId;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
