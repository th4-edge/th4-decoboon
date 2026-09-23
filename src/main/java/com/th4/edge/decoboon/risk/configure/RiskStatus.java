package com.th4.edge.decoboon.risk.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 风险处置状态。
 *
 * <p>依据用户 2026-09-23 裁决：不设「已处理待确认」中间态；关闭必须填写处置结果；
 * 预警条件自行消失进入 {@link #Void} 且不计入闭环率。</p>
 */
@Note("风险处置状态")
public enum RiskStatus implements Enumerable<String> {

    Pending("待响应"),
    Handling("处理中"),
    Closed("已关闭"),
    Void("已消解");

    private final String name;

    RiskStatus(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    /** 是否仍属于「待处理」清单。 */
    public boolean open() {
        return this == Pending || this == Handling;
    }
}
