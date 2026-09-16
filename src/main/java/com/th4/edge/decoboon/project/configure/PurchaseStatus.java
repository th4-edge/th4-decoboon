package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 材料采购订单状态。 */
@Note("采购状态")
public enum PurchaseStatus implements Enumerable<String> {
    Draft("草稿"), Submitted("待审批"), Approved("已审批"), Ordered("已下单"),
    PartiallyReceived("部分到场"), Received("已到场"), Rejected("已驳回"),
    Cancelled("已取消"), Closed("已关闭");
    private final String name;
    PurchaseStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
