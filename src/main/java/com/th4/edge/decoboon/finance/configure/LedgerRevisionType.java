package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 手工流水更正类型，用于审计追溯。 */
@Note("手工流水更正类型")
public enum LedgerRevisionType implements Enumerable<String> {
    MetadataCorrection("元数据更正"),
    ReverseAndRecreate("冲正重记");

    private final String name;

    LedgerRevisionType(String name) {
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
