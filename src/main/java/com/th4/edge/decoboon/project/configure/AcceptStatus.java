package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 验收单状态。
 */
@Note("验收状态")
public enum AcceptStatus implements Enumerable<String> {
    Pending("待确认"),
    Confirmed("已确认"),
    Rejected("已驳回");

    private final String name;

    AcceptStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
