package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 邀请状态。
 */
@Note("邀请状态")
public enum InviteStatus implements Attribute<String> {

    Pending("待接受"),
    Accepted("已接受"),
    Expired("已过期"),
    Cancelled("已取消"),
    Rejected("已拒绝");

    private final String name;

    InviteStatus(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getName() {
        return name;
    }
}
