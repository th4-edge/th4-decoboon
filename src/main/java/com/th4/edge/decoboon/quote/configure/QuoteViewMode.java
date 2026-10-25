package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价统一视图的服务端分组方式。 */
@Note("报价分组方式")
public enum QuoteViewMode implements Enumerable<String> {
    REGION("按区域"),
    TYPE("按类型");

    private final String name;

    QuoteViewMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
