package com.th4.edge.decoboon.ai.brief.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** AI 经营简报查询条件。 */
@Note("AI经营简报查询")
public class AiBriefQuery {

    @Note("统计月份任意一天，默认当前月")
    private LocalDate month;

    @Note("门店范围；为空表示企业全部门店")
    private Long storeId;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
