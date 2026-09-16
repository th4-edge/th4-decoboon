package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/** 平台用户凭证类型。 */
@Note("凭证类型")
public enum CredType implements Attribute<String> {
    Wx("微信"),
    Password("密码");

    private final String name;

    CredType(String name) {
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
