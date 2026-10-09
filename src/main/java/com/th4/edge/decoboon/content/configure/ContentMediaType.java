package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 内容素材媒体类型。 */
@Note("内容素材媒体类型")
public enum ContentMediaType implements Enumerable<String> {
    Image("图片"),
    Video("视频"),
    Audio("音频"),
    Text("文案");

    private final String name;

    ContentMediaType(String name) {
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
