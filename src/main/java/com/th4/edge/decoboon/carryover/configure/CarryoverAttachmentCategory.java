package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 承接项目附件分类。 */
@Note("承接附件分类")
public enum CarryoverAttachmentCategory implements Enumerable<String> {
    Contract("合同"),
    Quote("报价"),
    Site("现场"),
    Finance("财务"),
    Other("其他");

    private final String name;

    CarryoverAttachmentCategory(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return toString();
    }
}
