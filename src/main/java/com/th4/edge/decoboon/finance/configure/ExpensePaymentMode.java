package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 费用付款方式。 */
@Note("费用付款方式")
public enum ExpensePaymentMode implements Enumerable<String> {

    Direct("企业直付"),
    Reimbursement("员工报销");

    private final String name;

    ExpensePaymentMode(String name) {
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
