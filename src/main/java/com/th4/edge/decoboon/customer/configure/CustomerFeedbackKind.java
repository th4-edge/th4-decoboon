package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主反馈性质。 */
@Note("业主反馈性质")
public enum CustomerFeedbackKind implements Enumerable<String> {
    Opinion("意见"),
    Complaint("投诉");

    private final String name;

    CustomerFeedbackKind(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
