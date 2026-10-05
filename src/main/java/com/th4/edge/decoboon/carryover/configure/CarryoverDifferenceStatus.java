package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 双轨对账差异处理状态。 */
@Note("差异状态")
public enum CarryoverDifferenceStatus implements Enumerable<String> {
    Pending("待处理"),
    Handling("处理中"),
    Resolved("已解决"),
    Accepted("已确认接受");

    private final String name;

    CarryoverDifferenceStatus(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    @Override
    public String getValue() { return toString(); }
}
