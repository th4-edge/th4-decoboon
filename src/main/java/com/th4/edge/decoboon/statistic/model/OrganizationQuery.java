package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 组织效能查询条件。 */
@Note("组织效能查询")
public class OrganizationQuery {

    @Note("统计月份，任意一天代表所在月，默认当前月")
    private LocalDate month;

    @Note("门店范围；为空表示企业全部门店（仅企业负责人可跨门店）")
    private Long storeId;

    @Note("排行取前 N 名，默认 5，最大 20")
    private Integer topN;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getTopN() { return topN; }
    public void setTopN(Integer topN) { this.topN = topN; }
}
