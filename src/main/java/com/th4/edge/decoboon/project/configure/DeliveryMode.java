package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 采购到货路径。 */
@Note("采购交付方式")
public enum DeliveryMode implements Enumerable<String> {
    Direct("供应商直送工地"),
    Warehouse("入仓库");

    private final String name;

    DeliveryMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 只有明确入仓的采购才进入库存和批次流程。 */
    public boolean maintainsInventory() {
        return this == Warehouse;
    }
}
