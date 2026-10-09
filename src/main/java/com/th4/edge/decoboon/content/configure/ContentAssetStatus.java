package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 内容素材状态。 */
@Note("内容素材状态")
public enum ContentAssetStatus implements Enumerable<String> {
    Draft("待整理"),
    InternalReady("内部可用"),
    PublicReady("公开可用"),
    MarketingReady("宣传可用"),
    Expired("已过期"),
    Revoked("已撤回"),
    Blocked("已阻断");

    private final String name;

    ContentAssetStatus(String name) {
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
