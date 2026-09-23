package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主意见/投诉类型。 */
@Note("业主反馈类型")
public enum CustomerFeedbackType implements Enumerable<String> {
    Quality("质量问题"),
    Design("设计问题"),
    Material("材料问题"),
    Schedule("工期问题"),
    Service("服务问题"),
    Fee("费用问题"),
    Safety("安全问题"),
    Other("其他");

    private final String name;

    CustomerFeedbackType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
