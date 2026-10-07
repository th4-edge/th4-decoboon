package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 合同变更单状态。 */
@Note("合同变更状态")
public enum ContractChangeStatus implements Enumerable<String> {
    Pending("待审批"),
    Approved("审批通过"),
    /** 客户线下已确认且企业审批通过，合同与履约已联动；对外显示“已生效”。 */
    CustomerConfirmed("已生效"),
    Rejected("已驳回"),
    Cancelled("已取消");

    private final String name;

    ContractChangeStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
