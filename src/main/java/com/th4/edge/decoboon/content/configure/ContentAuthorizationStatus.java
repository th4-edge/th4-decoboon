package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 素材授权状态。 */
@Note("素材授权状态")
public enum ContentAuthorizationStatus implements Enumerable<String> {
    Pending("待审核"),
    Authorized("已授权"),
    Expiring("即将到期"),
    Expired("已过期"),
    Revoked("已撤回"),
    Disabled("已禁用");

    private final String name;

    ContentAuthorizationStatus(String name) {
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
