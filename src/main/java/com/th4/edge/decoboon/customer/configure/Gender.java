package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户性别。 */
@Note("客户性别")
public enum Gender implements Enumerable<String> {
    M("男"),
    F("女");

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
