package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 订单项类型。
 */
@Note("订单项类型")
public enum OrderItemType implements Attribute<String> {

    Plan("套餐"),
    Seats("席位包"),
    Stores("门店包");

    private final String name;

    OrderItemType(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getName() {
        return name();
    }
}
