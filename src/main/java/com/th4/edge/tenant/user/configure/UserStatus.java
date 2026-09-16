package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 平台用户状态。 */
@Note("用户状态")
public enum UserStatus implements Enumerable<String> {
    Active("正常"),
    Disabled("已停用"),
    Deleted("已注销");

    private final String name;

    UserStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
