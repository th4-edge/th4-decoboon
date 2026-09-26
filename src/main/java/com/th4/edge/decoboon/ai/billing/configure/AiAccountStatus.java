package com.th4.edge.decoboon.ai.billing.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("AI资源账户状态")
public enum AiAccountStatus implements Enumerable<String> {
    Active("正常"), Frozen("冻结");

    private final String name;
    AiAccountStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
