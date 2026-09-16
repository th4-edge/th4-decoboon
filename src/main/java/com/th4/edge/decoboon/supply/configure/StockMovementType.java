package com.th4.edge.decoboon.supply.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 库存变动类型。 */
@Note("库存变动类型")
public enum StockMovementType implements Enumerable<String> {
    Inbound("到货入库"),
    Issue("项目领料"),
    Loss("损耗"),
    ReturnIn("退料入库"),
    ReturnOut("退换供应商"),
    Adjustment("库存调整");

    private final String name;
    StockMovementType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
