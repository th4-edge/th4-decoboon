package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 内容任务状态。 */
@Note("内容任务状态")
public enum ContentTaskStatus implements Enumerable<String> {
    Draft("草稿"),
    Published("已发布"),
    Claimed("已有员工领取"),
    Completed("已完成"),
    Closed("已关闭");

    private final String name;

    ContentTaskStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public String getValue() { return name(); }
}
