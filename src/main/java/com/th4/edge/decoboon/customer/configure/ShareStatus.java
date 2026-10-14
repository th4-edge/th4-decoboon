package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/** 分享链接状态。 */
@Note("分享链接状态")
public enum ShareStatus implements Attribute<String> {

    Active("有效"),
    Revoked("已撤销"),
    Expired("已过期");

    private final String name;

    ShareStatus(String name) {
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
