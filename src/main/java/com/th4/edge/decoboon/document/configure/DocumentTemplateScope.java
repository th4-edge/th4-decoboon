package com.th4.edge.decoboon.document.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 单据模板来源范围。 */
@Note("单据模板范围")
public enum DocumentTemplateScope implements Enumerable<String> {
    Platform("平台行业基线"),
    Company("企业自定义");

    private final String name;

    DocumentTemplateScope(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
