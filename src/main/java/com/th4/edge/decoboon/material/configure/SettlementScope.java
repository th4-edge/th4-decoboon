package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 采购结算方式（DEC-073：两种方式并存 + 互斥 + 逐单优先 + 月结兜底）。
 *
 * <p>{@link #PerPurchaseOrder} 逐单结算：能确认单张采购单最终价时优先使用，提前锁定实际成本；
 * {@link #Monthly} 月结：期末按「供应商 + 期间」汇总，**只结算尚未逐单结算的差额**。</p>
 */
@Note("采购结算方式")
public enum SettlementScope implements Enumerable<String> {

    PerPurchaseOrder("逐单结算"),
    Monthly("月结（供应商+期间）");

    private final String name;

    SettlementScope(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
