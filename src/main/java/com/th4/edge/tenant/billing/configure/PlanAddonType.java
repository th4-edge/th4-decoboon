package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 套餐加购商品类型。
 */
@Note("套餐加购类型")
public enum PlanAddonType implements Attribute<String> {

    Seats("席位包"),
    Stores("门店包");

    private final String name;

    PlanAddonType(String name) {
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
