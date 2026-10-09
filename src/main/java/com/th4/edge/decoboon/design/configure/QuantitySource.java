package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 设计分项工程量来源：公式自动量或设计师手工量。 */
@Note("设计分项工程量来源")
public enum QuantitySource implements Enumerable<String> {
    Formula("公式"),
    Manual("手工");

    private final String name;

    QuantitySource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return toString();
    }
}
