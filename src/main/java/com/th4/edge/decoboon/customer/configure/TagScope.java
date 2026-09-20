package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标签来源类别。 */
@Note("标签来源类别")
public enum TagScope implements Enumerable<String> {

    System("系统"),
    Manual("人工");

    private final String name;

    TagScope(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
