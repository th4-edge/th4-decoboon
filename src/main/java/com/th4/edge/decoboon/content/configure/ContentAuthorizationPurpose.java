package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 素材授权用途。 */
@Note("素材授权用途")
public enum ContentAuthorizationPurpose implements Enumerable<String> {
    Internal("企业内使用"),
    Public("公开发布"),
    ZnDqMarketing("装得清宣传");

    private final String name;

    ContentAuthorizationPurpose(String name) {
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
