package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标签状态。 */
@Note("标签状态")
public enum TagStatus implements Enumerable<String> {

    Enabled("启用"),
    Disabled("停用");

    private final String name;

    TagStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
