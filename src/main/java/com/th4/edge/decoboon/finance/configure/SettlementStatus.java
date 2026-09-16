package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目结算状态。 */
@Note("结算状态")
public enum SettlementStatus implements Enumerable<String> {

    Draft("待确认"),
    Confirmed("已确认"),
    Closed("已关闭"),
    RetentionPending("待释放质保金"),
    RetentionReleased("质保金已释放");

    private final String name;

    SettlementStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
