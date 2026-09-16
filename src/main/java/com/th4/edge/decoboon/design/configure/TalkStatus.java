package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 谈单 PPT 状态。 */
@Note("谈单PPT状态")
public enum TalkStatus implements Enumerable<String> {
    Draft("草稿"),
    Generated("已生成"),
    Published("已发布"),
    Archived("已归档");

    private final String name;

    TalkStatus(String name) {
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
