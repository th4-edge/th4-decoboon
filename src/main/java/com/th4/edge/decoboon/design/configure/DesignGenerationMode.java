package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI设计方案生成模式。 */
@Note("AI设计方案生成模式")
public enum DesignGenerationMode implements Enumerable<String> {
    Precise("精准共创"),
    Concept("概念探索");

    private final String name;

    DesignGenerationMode(String name) {
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
