package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 家装可售项目统一业务类型。 */
@Note("家装项目类型")
public enum ItemType implements Enumerable<String> {
    Construction("施工"),
    Material("主材"),
    Soft("软装家具"),
    Custom("全屋定制"),
    Appliance("电器设备"),
    General("综合其他");

    private final String name;

    ItemType(String name) {
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
