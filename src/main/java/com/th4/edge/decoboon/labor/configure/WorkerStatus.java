package com.th4.edge.decoboon.labor.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 工人档案状态。 */
@Note("工人状态")
public enum WorkerStatus implements Enumerable<String> {
    Active("在职"),
    Disabled("停用");

    private final String name;

    WorkerStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
