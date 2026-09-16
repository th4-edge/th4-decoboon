package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户跟进方式。 */
@Note("客户跟进方式")
public enum FollowWay implements Enumerable<String> {
    Phone("电话"),
    Visit("到店或拜访"),
    Wechat("微信"),
    Video("视频"),
    Other("其他");

    private final String name;

    FollowWay(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
