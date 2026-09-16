package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 当前登录会话身份。 */
@Note("会话身份")
public enum SessionType implements Enumerable<String> {

    User("未加入企业"),
    Employee("企业员工"),
    Customer("业主客户");

    private final String name;

    SessionType(String name) {
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
