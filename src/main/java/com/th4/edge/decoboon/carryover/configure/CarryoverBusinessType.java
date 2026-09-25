package com.th4.edge.decoboon.carryover.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 触发承接项目锁定的首笔业务类型。 */
@Note("承接首笔业务类型")
public enum CarryoverBusinessType implements Enumerable<String> {
    Receipt("收款"),
    Payment("付款"),
    FinanceAdjustment("财务调整"),
    Task("施工任务"),
    Stage("施工阶段"),
    Acceptance("阶段验收"),
    ContractChange("合同变更"),
    Purchase("采购"),
    GoodsReceipt("到货"),
    SiteReport("现场汇报"),
    Other("其他业务");

    private final String name;

    CarryoverBusinessType(String name) {
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
