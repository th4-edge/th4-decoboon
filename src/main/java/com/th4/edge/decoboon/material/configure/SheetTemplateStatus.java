package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 主材下单单据模板状态。 */
@Note("主材下单模板状态")
public enum SheetTemplateStatus implements Enumerable<String> {

    Active("启用"),
    Disabled("停用");

    private final String name;

    SheetTemplateStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
