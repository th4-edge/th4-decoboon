package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业负责人首页经营指示灯状态。 */
@Note("经营指示灯状态")
public enum WorkbenchSignalStatus implements Enumerable<String> {
    Normal("正常"),
    Attention("需关注"),
    Danger("异常"),
    Unknown("暂不可判断");

    private final String name;

    WorkbenchSignalStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
