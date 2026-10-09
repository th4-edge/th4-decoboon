package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 授权主体类型。 */
@Note("授权主体类型")
public enum ContentAuthorizationSubject implements Enumerable<String> {
    Company("企业"),
    Customer("客户"),
    Employee("员工");

    private final String name;

    ContentAuthorizationSubject(String name) {
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
