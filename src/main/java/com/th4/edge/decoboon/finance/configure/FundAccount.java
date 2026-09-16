package com.th4.edge.decoboon.finance.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 资金账户。 */
@Note("资金账户")
public enum FundAccount implements Enumerable<String> {

    CompanyBank("企业对公"),
    Cash("现金"),
    Wechat("微信"),
    Alipay("支付宝"),
    Other("其他账户");

    private final String name;

    FundAccount(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
