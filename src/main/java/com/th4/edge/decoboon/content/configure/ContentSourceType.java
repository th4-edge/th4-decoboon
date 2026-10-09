package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 内容素材来源。 */
@Note("内容素材来源")
public enum ContentSourceType implements Enumerable<String> {
    Camera("现场拍摄"),
    Album("相册导入"),
    Project("项目事实"),
    Import("系统导入");

    private final String name;

    ContentSourceType(String name) {
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
