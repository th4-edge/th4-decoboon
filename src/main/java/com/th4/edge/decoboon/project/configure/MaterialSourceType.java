package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 材料计划来源。 */
@Note("材料计划来源")
public enum MaterialSourceType implements Enumerable<String> {

    Selection("合同选材确认"),
    ChangeAdd("增减项新增主材"),
    ChangeReduce("增减项扣减主材");

    private final String name;

    MaterialSourceType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
