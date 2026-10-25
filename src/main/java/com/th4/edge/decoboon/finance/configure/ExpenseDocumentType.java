package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 费用证据附件类型；不代表发票管理。 */
@Note("费用证据类型")
public enum ExpenseDocumentType implements Enumerable<String> {

    Invoice("发票"),
    Receipt("收据"),
    PaymentScreenshot("支付截图"),
    Itinerary("行程单"),
    Other("其他说明");

    private final String name;

    ExpenseDocumentType(String name) {
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
