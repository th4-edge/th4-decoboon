package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("PayrollItemAttribution")
public enum PayrollItemAttribution implements Enumerable<String> {

    Project("项目"),
    Company("企业"),
    ;

    private final String name;

    PayrollItemAttribution(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
