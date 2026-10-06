package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 流水来源。 */
@Note("流水来源")
public enum LedgerSource implements Enumerable<String> {

    Receipt("客户收款"),
    Refund("客户退款"),
    Payment("业务付款"),
    Manual("手工记账"),
    Adjustment("调整冲销");

    private final String name;

    LedgerSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
