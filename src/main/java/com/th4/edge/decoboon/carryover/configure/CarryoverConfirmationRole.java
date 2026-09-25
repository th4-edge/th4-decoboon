package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 承接项目需要确认的业务角色。 */
@Note("承接确认角色")
public enum CarryoverConfirmationRole implements Enumerable<String> {
    Boss("企业负责人"),
    Finance("财务"),
    Pm("项目经理"),
    Designer("设计师");

    private final String name;

    CarryoverConfirmationRole(String name) {
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
