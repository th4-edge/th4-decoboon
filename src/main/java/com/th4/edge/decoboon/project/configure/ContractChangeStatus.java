package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 合同变更单状态。
 *
 * <p>DEC-068：业务状态只有 {@code Pending → CustomerConfirmed / Rejected / Cancelled}。
 * 「业主确认」不再建模为系统事实；{@code Approved} 为历史遗留的“待客户确认”中间态，
 * 新单据不再产生，仅存量数据可经一次性收口进入 {@code CustomerConfirmed}。
 * {@code CustomerConfirmed} 为历史字面量，业务语义即“已生效”，对外统一显示“已生效”。</p>
 */
@Note("合同变更状态")
public enum ContractChangeStatus implements Enumerable<String> {
    Pending("待审批"),
    /** 历史遗留：旧口径“审批通过、待客户确认”，新单据不再产生。 */
    Approved("审批通过"),
    /** 已生效：企业审批通过且合同与履约已联动（历史字面量，业务语义=已生效）。 */
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
