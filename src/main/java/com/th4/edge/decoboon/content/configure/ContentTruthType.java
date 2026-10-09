package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 素材真实性类型。 */
@Note("素材真实性类型")
public enum ContentTruthType implements Enumerable<String> {
    Native("真实"),
    Test("测试"),
    Demo("演示"),
    Hypothesis("待验证");

    private final String name;

    ContentTruthType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
