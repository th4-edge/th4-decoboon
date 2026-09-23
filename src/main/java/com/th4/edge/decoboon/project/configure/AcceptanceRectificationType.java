package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 阶段验收整改类型。截止天数由服务端按类型固定计算，业主和业务人员不可直接设置。 */
@Note("验收整改类型")
public enum AcceptanceRectificationType implements Enumerable<String> {
    Construction("施工整改", 3),
    Design("设计整改", 5);

    private final String name;
    private final int dueDays;

    AcceptanceRectificationType(String name, int dueDays) {
        this.name = name;
        this.dueDays = dueDays;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getDueDays() {
        return dueDays;
    }
}
