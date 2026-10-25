package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户流失作用范围。 */
@Note("客户流失范围")
public enum CustomerLossScope implements Enumerable<String> {
    Customer("客户整体"),
    Property("本次房屋");

    private final String name;

    CustomerLossScope(String name) {
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
