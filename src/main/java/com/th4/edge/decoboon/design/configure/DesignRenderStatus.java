package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI效果图版本状态。 */
@Note("AI效果图版本状态")
public enum DesignRenderStatus implements Enumerable<String> {
    Ready("已生成"),
    Failed("生成失败"),
    Superseded("已被替代");

    private final String name;

    DesignRenderStatus(String name) {
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
