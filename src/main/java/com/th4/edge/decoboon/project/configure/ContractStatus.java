package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 项目合同基线状态。 */
@Note("合同状态")
public enum ContractStatus implements Enumerable<String> {
    Draft("草稿"),
    PendingSign("待签约"),
    Signed("已签约"),
    Effective("已生效"),
    Active("已生效"),
    Changing("变更中"),
    Changed("已变更"),
    Completed("已完工"),
    Settled("已结算"),
    Closed("已关闭"),
    Terminated("已终止");

    private final String name;

    ContractStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
