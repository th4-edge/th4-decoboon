package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/** 登录后待处理邀请类型。 */
@Note("待处理邀请类型")
public enum EntryInviteType implements Attribute<String> {

    Employee("员工邀请"),
    Customer("业主邀请");

    private final String name;

    EntryInviteType(String name) {
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
