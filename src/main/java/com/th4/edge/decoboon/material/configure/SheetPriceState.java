package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 价格状态（BR-045）。
 *
 * <p>下单时可能没有价格（多供应商商务合作、月结、结算时才定价）：
 * 无价可下单与到场，价格字段留空而**不是 0**；成本在结算时收敛。
 * {@code costComplete=false} 时利润与毛利率必须为 {@code null}。</p>
 */
@Note("价格状态")
public enum SheetPriceState implements Enumerable<String> {

    Priced("已定价"),
    Unpriced("未定价（待结算）"),
    Settled("已结算"),
    Mixed("混合（部分已定价）");

    private final String name;

    SheetPriceState(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 是否可用于计算实际成本（结算价优先，其次采购价）。 */
    public boolean costUsable() {
        return this == Priced || this == Settled || this == Mixed;
    }
}
