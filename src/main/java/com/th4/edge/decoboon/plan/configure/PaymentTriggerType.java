package com.th4.edge.decoboon.plan.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 收款节点触发依据。 */
@Note("收款触发类型")
public enum PaymentTriggerType implements Enumerable<String> {
    ContractSigned("合同签约"),
    StageStarted("阶段开工"),
    StageDone("阶段施工完成"),
    StageAccepted("阶段验收完成"),
    ProjectFinished("项目竣工");

    private final String name;
    PaymentTriggerType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
