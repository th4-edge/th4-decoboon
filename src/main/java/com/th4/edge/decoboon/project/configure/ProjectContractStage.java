package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 合同履约阶段（企业负责人项目经营台账的合同维度分段）。
 *
 * <p>只做合同状态的分组投影，不新增合同状态机。</p>
 */
@Note("合同履约阶段")
public enum ProjectContractStage implements Enumerable<String> {
    Pending("待签约"),
    Active("履约中"),
    Completed("已完成");

    private final String name;

    ProjectContractStage(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 合同状态到履约阶段的唯一映射。 */
    public static ProjectContractStage of(ContractStatus status) {
        if (status == null) {
            return Pending;
        }
        return switch (status) {
            case Draft, PendingSign -> Pending;
            case Signed, Effective, Active, Changing, Changed -> Active;
            case Completed, Settled, Closed, Terminated -> Completed;
        };
    }
}
