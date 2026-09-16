package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 应付业务类型。 */
@Note("应付类型")
public enum PayableType implements Enumerable<String> {

    Material("材料款"),
    Salary("工人工资"),
    Project("项目款"),
    Supplier("供应商款"),
    Expense("费用支出"),
    Other("其他应付");

    private final String name;

    PayableType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
