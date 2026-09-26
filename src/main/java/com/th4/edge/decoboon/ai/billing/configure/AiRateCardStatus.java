package com.th4.edge.decoboon.ai.billing.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("AI费率版本状态")
public enum AiRateCardStatus implements Enumerable<String> {
    Draft("草稿"), Active("生效"), Disabled("停用");

    private final String name;
    AiRateCardStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
