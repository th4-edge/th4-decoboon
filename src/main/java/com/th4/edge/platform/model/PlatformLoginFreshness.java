package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业负责人登录活跃筛选。 */
@Note("企业负责人登录活跃状态")
public enum PlatformLoginFreshness implements Enumerable<String> {
    All("全部"),
    Today("今日已登录"),
    Within7Days("7日内登录"),
    Inactive7Days("7日未登录"),
    Inactive30Days("30日未登录"),
    Never("从未登录");

    private final String name;

    PlatformLoginFreshness(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
