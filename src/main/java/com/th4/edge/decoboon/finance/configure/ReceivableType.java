package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 客户应收的业务类型。 */
@Note("应收类型")
public enum ReceivableType implements Enumerable<String> {

    First("首期款"),
    Progress("进度款"),
    Final("尾款"),
    Retention("质保金"),
    Other("其他应收");

    private final String name;

    ReceivableType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
