package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 收款冲正原因分类。 */
@Note("收款冲正原因")
public enum ReceiptReversalReasonType implements Enumerable<String> {
    NotReceived("未实际到账"),
    AmountError("金额录错"),
    ReceivableError("应收节点选错"),
    Duplicate("重复登记"),
    AccountOrDateError("账户或日期录错"),
    Other("其他");

    private final String name;

    ReceiptReversalReasonType(String name) {
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
