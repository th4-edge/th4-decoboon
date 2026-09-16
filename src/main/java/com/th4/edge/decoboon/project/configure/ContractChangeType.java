package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 合同变更类型。 */
@Note("合同变更类型")
public enum ContractChangeType implements Enumerable<String> {
    Add("增项"),
    Remove("减项"),
    Replace("替换"),
    Quantity("数量变更"),
    Price("价格变更");

    private final String name;
    ContractChangeType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
