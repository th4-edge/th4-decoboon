package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主意见/投诉处理状态。 */
@Note("业主反馈状态")
public enum CustomerFeedbackStatus implements Enumerable<String> {
    Submitted("待受理"),
    Processing("处理中"),
    Resolved("已回复"),
    Escalated("已升级"),
    Closed("已关闭");

    private final String name;

    CustomerFeedbackStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
