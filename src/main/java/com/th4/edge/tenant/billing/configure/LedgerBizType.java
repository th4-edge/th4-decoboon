package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 余额流水业务类型。
 */
@Note("余额流水类型")
public enum LedgerBizType implements Attribute<String> {

    TrialGrant("试用发放"),
    RechargeGrant("续费到账"),
    ManualAdjust("人工调整");

    private final String name;

    LedgerBizType(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getName() {
        return name;
    }
}
