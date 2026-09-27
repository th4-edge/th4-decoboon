package com.th4.edge.decoboon.ai.billing.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("AI点数账本类型")
public enum AiLedgerType implements Enumerable<String> {
    Grant("发放"), Reserve("预占"), Consume("消耗"), Release("释放"),
    Refund("返还"), Expire("过期"), Adjust("调整");

    private final String name;
    AiLedgerType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
