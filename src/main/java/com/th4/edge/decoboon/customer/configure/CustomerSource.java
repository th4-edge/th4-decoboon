package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户来源。 */
@Note("客户来源")
public enum CustomerSource implements Enumerable<String> {
    Lead("短视频线索"),
    Referral("转介绍"),
    WalkIn("自然到店"),
    OldCustomer("老客户"),
    Video("视频咨询"),
    Other("其他");

    private final String name;

    CustomerSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
