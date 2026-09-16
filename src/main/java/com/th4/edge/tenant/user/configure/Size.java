package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

@Note("企业规模")
public enum Size implements Attribute<String> {
    A("10人以下"),
    B("10人-50人"),
    C("50人-100人"),
    D("100人以上");

    private final String name;

    Size(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return toString();
    }

    @Override
    public String getName() {
        return name;
    }
}
