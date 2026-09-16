package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 员工企业归属状态。 */
@Note("员工状态")
public enum EmployeeStatus implements Enumerable<String> {
    Active("在职"),
    Suspended("停用"),
    Resigned("已离职");

    private final String name;

    EmployeeStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
