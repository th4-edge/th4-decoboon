package com.th4.edge.decoboon.common.file.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 私有文件所属业务对象。 */
@Note("私有文件业务范围")
public enum FileScope implements Enumerable<String> {

    Report("现场动态"),
    Acceptance("阶段验收"),
    Feedback("业主意见投诉"),
    ContractChange("合同变更"),
    Design("设计方案"),
    Talk("谈单PPT"),
    Measure("量房记录"),
    Task("施工任务"),
    GoodsReceipt("材料到场"),
    StockMovement("库存流水"),
    Catalog("材料库"),
    Carryover("业务承接"),
    Document("单据归档");

    private final String name;

    FileScope(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
