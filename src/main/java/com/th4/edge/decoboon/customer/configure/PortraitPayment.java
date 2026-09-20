package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-付款方式偏好。 */
@Note("画像-付款方式偏好")
public enum PortraitPayment implements Enumerable<String> {

    Full("全款"),
    Installment("分期"),
    Loan("贷款"),
    Unknown("待确认");

    private final String name;

    PortraitPayment(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
