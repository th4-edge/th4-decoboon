package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户分级。 */
@Note("客户分级")
public enum CustomerGradeLevel implements Enumerable<String> {

    A("A级"),
    B("B级"),
    C("C级");

    private final String name;

    CustomerGradeLevel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
