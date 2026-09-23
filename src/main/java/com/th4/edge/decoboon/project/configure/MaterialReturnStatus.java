package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 材料退货单状态。 */
@Note("材料退货单状态")
public enum MaterialReturnStatus implements Enumerable<String> {

    Pending("待协商"),
    Confirmed("已退货"),
    Cancelled("已取消");

    private final String name;

    MaterialReturnStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
