package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/** 分享范围：决定脱敏白名单（Friend 不含金额与房号）。 */
@Note("分享范围")
public enum ShareScope implements Attribute<String> {

    Family("家人"),
    Friend("好友");

    private final String name;

    ShareScope(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name;
    }

    public String getName() {
        return name;
    }
}
