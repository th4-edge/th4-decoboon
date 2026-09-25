package com.th4.edge.decoboon.common.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业务数据来源。存量与正常新业务为 New，历史承接基线为 Carryover。 */
@Note("业务数据来源")
public enum BusinessOrigin implements Enumerable<String> {
    New("正常业务"),
    Carryover("接管基线");

    private final String name;

    BusinessOrigin(String name) {
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
