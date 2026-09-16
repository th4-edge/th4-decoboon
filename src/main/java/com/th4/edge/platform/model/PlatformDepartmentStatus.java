package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 平台部门状态。 */
@Note("平台部门状态")
public enum PlatformDepartmentStatus implements Enumerable<String> {
    Active("启用"),
    Suspended("停用");

    private final String name;
    PlatformDepartmentStatus(String name) { this.name = name; }
    @Override public String getValue() { return name(); }
    @Override public String getName() { return name; }
}
