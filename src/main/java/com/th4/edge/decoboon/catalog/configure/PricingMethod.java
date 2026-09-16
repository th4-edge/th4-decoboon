package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 可售项目统一计价方式。 */
@Note("项目计价方式")
public enum PricingMethod implements Enumerable<String> {
    MaterialAndManual("材料+人工"),
    MaterialOnly("仅材料"),
    ManualOnly("仅人工"),
    FixedTotal("固定总价");

    private final String name;

    PricingMethod(String name) {
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
