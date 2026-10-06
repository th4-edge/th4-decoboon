package com.th4.edge.decoboon.document.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业单据模板版本状态。 */
@Note("单据模板版本状态")
public enum DocumentTemplateVersionStatus implements Enumerable<String> {
    Draft("草稿"),
    Published("已发布"),
    Superseded("已替代"),
    Disabled("已停用");

    private final String name;

    DocumentTemplateVersionStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
