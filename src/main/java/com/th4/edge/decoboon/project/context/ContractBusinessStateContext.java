package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.common.model.BusinessDerivedLabel;
import com.th4.edge.decoboon.common.model.BusinessNextAction;
import com.th4.edge.decoboon.common.model.BusinessStateView;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 合同服务端状态视图。 */
@Note("合同服务端状态视图")
@Component
public class ContractBusinessStateContext {

    public void enrich(ProjectContract contract) {
        if (contract == null || contract.getContractId() == null || contract.getStatus() == null) {
            return;
        }
        BusinessStateView view = new BusinessStateView();
        view.setObjectKey("contract");
        view.setObjectId(contract.getContractId());
        view.setStatus(contract.getStatus().name());
        view.setStatusLabel(contract.getStatus().getName());
        view.setStatusTone(tone(contract.getStatus()));
        view.setDerivedLabels(derivedLabels(contract));
        view.setNextAction(nextAction(contract));
        view.setBlockedReasons(List.of());
        view.setNextOwner(nextOwner(contract.getStatus()));
        view.setUpdatedAt(contract.getUpdateTime() == null ? null : contract.getUpdateTime().toLocalDateTime());
        view.setBusinessRefs(List.of("BR-002", "BR-010"));
        contract.setBusinessStateView(view);
    }

    private List<BusinessDerivedLabel> derivedLabels(ProjectContract contract) {
        List<BusinessDerivedLabel> labels = new ArrayList<>();
        if (contract.getSignedAt() != null) {
            labels.add(new BusinessDerivedLabel("signed", "已签约", "progress"));
        }
        if (contract.getEffectiveAt() != null) {
            labels.add(new BusinessDerivedLabel("effective", "已生效", "done"));
        }
        if (contract.getProject() != null && contract.getProject().getProjectId() != null) {
            labels.add(new BusinessDerivedLabel("projectCreated", "项目已创建", "done"));
        }
        if (contract.getPendingChangeCount() != null && contract.getPendingChangeCount() > 0) {
            labels.add(new BusinessDerivedLabel("pendingChange", "待处理变更 " + contract.getPendingChangeCount(), "pending"));
        }
        return labels;
    }

    private BusinessNextAction nextAction(ProjectContract contract) {
        BusinessNextAction action = new BusinessNextAction();
        action.setFromStatus(contract.getStatus().name());
        action.setBusinessRefs(List.of("BR-002", "BR-010"));
        action.setBlockedReasons(List.of());
        action.setMissingFields(List.of());
        switch (contract.getStatus()) {
            case Draft, PendingSign -> {
                action.setKey("contract/sign");
                action.setLabel("确认签约");
                action.setHint("确认最终签约价格并完成合同签署。");
                action.setOwnerRole("企业负责人 / 店长");
                action.setToStatus("Signed");
                action.setReloadScopes(List.of("contract", "customer", "property", "quote", "receivable"));
            }
            case Signed -> {
                action.setKey("contract/effective");
                action.setLabel("确认生效");
                action.setHint("合同已签署，确认生效后生成唯一项目。");
                action.setOwnerRole("企业负责人 / 店长");
                action.setToStatus("Effective");
                action.setReloadScopes(List.of("contract", "quote", "project", "receivable", "materials"));
            }
            case Effective, Active -> {
                action.setKey("project/open");
                action.setLabel("查看项目");
                action.setHint("合同已生效，进入项目履约、收款和变更。");
                action.setOwnerRole("项目参与角色");
                action.setToStatus("Effective");
                action.setReloadScopes(List.of("contract", "project", "receivable", "materials"));
            }
            case Changing -> {
                action.setKey("change/review");
                action.setLabel("处理合同变更");
                action.setHint("完成变更审批并回读合同、任务、材料、应收和成本。");
                action.setOwnerRole("企业负责人 / 财务");
                action.setToStatus("Changed");
                action.setReloadScopes(List.of("contractChange", "contract", "project", "projectTask", "materials", "receivable", "cost"));
            }
            case Changed -> {
                action.setKey("project/open");
                action.setLabel("查看变更后履约");
                action.setHint("变更已生效，核对任务、材料、应收和成本。");
                action.setOwnerRole("项目经理 / 财务");
                action.setToStatus("Changed");
                action.setReloadScopes(List.of("contract", "project", "projectTask", "materials", "receivable", "cost"));
            }
            case Completed -> {
                action.setKey("settlement/open");
                action.setLabel("进入结算");
                action.setHint("合同已完工，进入项目结算。");
                action.setOwnerRole("财务");
                action.setToStatus("Settled");
                action.setReloadScopes(List.of("contract", "project", "receivable", "payable", "cost"));
            }
            case Settled -> {
                action.setKey("contract/archive");
                action.setLabel("查看归档");
                action.setHint("合同已结算，只读查看合同档案。");
                action.setOwnerRole("财务 / 企业负责人");
                action.setToStatus("Settled");
                action.setEnabled(Boolean.FALSE);
                action.setReloadScopes(List.of("contract", "project"));
            }
            default -> {
                action.setKey("contract/closed-view");
                action.setLabel("查看关闭原因");
                action.setHint("合同已关闭、终止或作废，仅保留历史查看。");
                action.setOwnerRole("企业负责人 / 财务");
                action.setToStatus(contract.getStatus().name());
                action.setEnabled(Boolean.FALSE);
                action.setReloadScopes(List.of("contract"));
            }
        }
        return action;
    }

    private String nextOwner(ContractStatus status) {
        return switch (status) {
            case Draft, PendingSign, Signed -> "企业负责人 / 店长";
            case Effective, Active, Changing, Changed -> "项目经理 / 财务";
            default -> "财务 / 企业负责人";
        };
    }

    private String tone(ContractStatus status) {
        if (status == ContractStatus.Terminated) return "blocked";
        return switch (status) {
            case Draft -> "todo";
            case PendingSign -> "pending";
            case Signed -> "progress";
            case Effective, Active -> "done";
            case Changing -> "alert";
            case Changed -> "special";
            case Completed, Settled -> "dark";
            default -> "closed";
        };
    }
}
