package com.th4.edge.decoboon.ai.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI 影子账本状态。P0 不产生真实扣点。 */
@Note("AI影子账本状态")
public enum AiBillingStatus implements Enumerable<String> {

    Pending("待结算"),
    Calculated("已计算"),
    Returned("已返还"),
    Void("已作废"),
    Adjusted("已调整");

    private final String name;

    AiBillingStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
