package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价行在报价单中的业务角色。 */
@Note("报价行角色")
public enum QuoteLineRole implements Enumerable<String> {
    Base("基础项目"),
    Option("可选项目"),
    Upgrade("升级项目"),
    Add("新增项目"),
    Deduct("扣减项目"),
    Replace("替换项目"),
    Gift("赠送项目");

    private final String name;

    QuoteLineRole(String name) {
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
