package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 数据范围级别。
 */
@Note("数据范围级别")
public enum DataScopeType implements Enumerable<String> {

    Company("企业"),
    Store("门店"),
    Team("团队"),
    Self("个人");

    private final String name;

    DataScopeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
