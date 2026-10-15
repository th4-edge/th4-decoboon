package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 采购结算单状态。
 *
 * <p>P3 只实现"确认即生效"：确认后回填结算价、把暂估应付修正为确定金额、补齐实际成本；
 * 撤销走反向记录（`Cancelled`），不覆盖历史事实。</p>
 */
@Note("采购结算单状态")
public enum SettlementStatus implements Enumerable<String> {

    Confirmed("已确认"),
    Cancelled("已撤销");

    private final String name;

    SettlementStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
