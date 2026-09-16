package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 订单状态。
 */
@Note("订单状态")
public enum OrderStatus implements Attribute<String> {

    Pending("待支付"),
    Paid("已支付"),
    Granted("已履约"),
    Expired("已过期"),
    Cancelled("已取消"),
    Refunded("已退款");

    private final String name;

    OrderStatus(String name) {
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
