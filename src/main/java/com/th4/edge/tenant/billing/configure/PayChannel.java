package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 支付渠道。
 */
@Note("支付渠道")
public enum PayChannel implements Attribute<String> {

    Wx("微信支付"),
    Alipay("支付宝"),
    Offline("线下转账");

    private final String name;

    PayChannel(String name) {
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
