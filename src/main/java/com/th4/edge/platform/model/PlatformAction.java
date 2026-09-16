package com.th4.edge.platform.model;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/** 平台人工运营动作。 */
@Note("平台操作动作")
public enum PlatformAction implements Attribute<String> {
    Renew("人工续费"),
    PlanChange("调整套餐"),
    SeatAdjust("调整席位"),
    StoreAdjust("调整门店"),
    StatusChange("调整订阅状态"),
    Freeze("冻结订阅"),
    Unfreeze("恢复订阅");

    private final String name;

    PlatformAction(String name) {
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
