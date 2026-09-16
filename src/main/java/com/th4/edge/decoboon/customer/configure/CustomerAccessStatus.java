package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主项目授权状态。 */
@Note("业主项目授权状态")
public enum CustomerAccessStatus implements Enumerable<String> {

    Active("有效"),
    Revoked("已撤销");

    private final String name;

    CustomerAccessStatus(String name) {
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
