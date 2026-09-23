package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业负责人经营动态类型。 */
@Note("经营动态类型")
public enum BusinessEventType implements Enumerable<String> {
    CustomerCreated(BusinessEventCategory.Customer, "新增客户", BusinessEventLevel.Normal),
    CustomerFollowed(BusinessEventCategory.Customer, "客户跟进", BusinessEventLevel.Normal),
    QuoteSubmitted(BusinessEventCategory.Customer, "报价提交", BusinessEventLevel.Attention),
    QuoteAccepted(BusinessEventCategory.Customer, "客户接受报价", BusinessEventLevel.Milestone),
    QuoteRejected(BusinessEventCategory.Customer, "客户驳回报价", BusinessEventLevel.Warning),
    QuoteConverted(BusinessEventCategory.Customer, "报价转项目", BusinessEventLevel.Milestone),
    ContractSigned(BusinessEventCategory.Contract, "合同签约", BusinessEventLevel.Milestone),
    ContractEffective(BusinessEventCategory.Contract, "合同生效", BusinessEventLevel.Milestone),
    ContractChangeSubmitted(BusinessEventCategory.Contract, "提交合同变更", BusinessEventLevel.Attention),
    ContractChangeApproved(BusinessEventCategory.Contract, "合同变更生效", BusinessEventLevel.Warning),
    ContractChangeRejected(BusinessEventCategory.Contract, "合同变更驳回", BusinessEventLevel.Warning),
    StageStarted(BusinessEventCategory.Project, "阶段开工", BusinessEventLevel.Milestone),
    TaskCompleted(BusinessEventCategory.Project, "任务完成", BusinessEventLevel.Normal),
    ProjectProgress(BusinessEventCategory.Project, "施工进展", BusinessEventLevel.Normal),
    AcceptanceRequested(BusinessEventCategory.Project, "申请验收", BusinessEventLevel.Attention),
    AcceptanceCompleted(BusinessEventCategory.Project, "验收通过", BusinessEventLevel.Milestone),
    AcceptanceRejected(BusinessEventCategory.Project, "验收驳回", BusinessEventLevel.Danger),
    IssueReported(BusinessEventCategory.Project, "现场问题", BusinessEventLevel.Danger),
    MaterialArrived(BusinessEventCategory.Project, "材料到场", BusinessEventLevel.Normal),
    ProjectNotice(BusinessEventCategory.Project, "项目通知", BusinessEventLevel.Attention),
    ReceiptReceived(BusinessEventCategory.Finance, "收到工程款", BusinessEventLevel.Milestone),
    PaymentPaid(BusinessEventCategory.Finance, "项目付款", BusinessEventLevel.Normal);

    private final BusinessEventCategory category;
    private final String name;
    private final BusinessEventLevel level;

    BusinessEventType(BusinessEventCategory category, String name, BusinessEventLevel level) {
        this.category = category;
        this.name = name;
        this.level = level;
    }

    public BusinessEventCategory getCategory() {
        return category;
    }

    public BusinessEventLevel getLevel() {
        return level;
    }

    @Override
    public String getName() {
        return name;
    }
}
