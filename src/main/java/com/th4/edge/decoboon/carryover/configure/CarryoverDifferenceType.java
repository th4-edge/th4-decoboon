package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 双轨对账差异分类。 */
@Note("差异分类")
public enum CarryoverDifferenceType implements Enumerable<String> {
    Time("时间差异"),
    Entity("主体差异"),
    Tax("含税差异"),
    Scope("口径差异"),
    MissingData("数据缺失");

    private final String name;

    CarryoverDifferenceType(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    @Override
    public String getValue() { return toString(); }
}
