package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 供应商账期模式。 */
@Note("账期模式")
public enum SettlementMode implements Enumerable<String> {

    /** 收货日 + 账期天数（如收货后 30 天付款）。 */
    ReceiptDays("收货日加账期天数"),

    /** 对账月结：按每月固定截账日结算（如每月 25 日截账、次月付款）。 */
    MonthlyCutoff("对账月结日");

    private final String name;

    SettlementMode(String name) {
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
