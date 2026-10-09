package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 内容发布平台。 */
@Note("内容发布平台")
public enum ContentPlatform implements Enumerable<String> {
    Douyin("抖音"),
    WeChatChannels("视频号"),
    Xiaohongshu("小红书"),
    Other("其他");

    private final String name;

    ContentPlatform(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public String getValue() { return name(); }
}
