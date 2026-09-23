package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 主材选材记录状态。 */
@Note("主材选材状态")
public enum SelectionStatus implements Enumerable<String> {

    Draft("草稿"),
    PendingConfirm("待客户确认"),
    Confirmed("已确认");

    private final String name;

    SelectionStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 已确认版本不允许再原地修改，变更必须形成新版本。 */
    public boolean locked() {
        return this == Confirmed;
    }
}
