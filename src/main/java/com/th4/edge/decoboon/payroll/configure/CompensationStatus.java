package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("CompensationStatus")
public enum CompensationStatus implements Enumerable<String> {

    Draft("草稿"),
    Active("生效"),
    Expired("失效"),
    ;

    private final String name;

    CompensationStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
