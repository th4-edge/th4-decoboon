package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户状态变更来源类型。 */
@Note("客户状态变更类型")
public enum CustomerStatusChangeType implements Enumerable<String> {
    Dormant("设为休眠"),
    Restore("恢复跟进"),
    FollowWake("写跟进唤醒"),
    OwnerChange("变更负责人"),
    Lost("标记流失"),
    RestoreLost("恢复流失客户");

    private final String name;

    CustomerStatusChangeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
