package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.common.model.BusinessDerivedLabel;
import com.th4.edge.decoboon.common.model.BusinessNextAction;
import com.th4.edge.decoboon.common.model.BusinessStateView;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 项目服务端状态视图。 */
@Note("项目服务端状态视图")
@Component
public class ProjectBusinessStateContext {

    public void enrich(Project project) {
        if (project == null || project.getProjectId() == null || project.getStatus() == null) {
            return;
        }
        BusinessStateView view = new BusinessStateView();
        view.setObjectKey("project");
        view.setObjectId(project.getProjectId());
        view.setStatus(project.getStatus().name());
        view.setStatusLabel(project.getStatus().getName());
        view.setStatusTone(tone(project.getStatus()));
        view.setDerivedLabels(derivedLabels(project));
        view.setNextAction(nextAction(project));
        view.setBlockedReasons(project.getRiskReason() == null ? List.of() : List.of(project.getRiskReason()));
        view.setNextOwner(nextOwner(project.getStatus()));
        view.setUpdatedAt(project.getUpdateTime() == null ? null : project.getUpdateTime().toLocalDateTime());
        view.setBusinessRefs(List.of("BR-008", "BR-010", "SCN-003"));
        project.setBusinessStateView(view);
    }

    private List<BusinessDerivedLabel> derivedLabels(Project project) {
        List<BusinessDerivedLabel> labels = new ArrayList<>();
        if (project.getCurrentStageName() != null && !project.getCurrentStageName().isBlank()) {
            labels.add(new BusinessDerivedLabel("currentStage", project.getCurrentStageName(), "info"));
        }
        if (project.getProgress() != null) {
            labels.add(new BusinessDerivedLabel("progress", project.getProgress() + "%", "progress"));
        }
        if (Boolean.FALSE.equals(project.getCostComplete())) {
            labels.add(new BusinessDerivedLabel("costIncomplete", "成本未完整", "pending"));
        }
        if (project.getRiskLevel() != null) {
            labels.add(new BusinessDerivedLabel("risk", project.getRiskLevel().getName(), "alert"));
        }
        return labels;
    }

    private BusinessNextAction nextAction(Project project) {
        BusinessNextAction action = new BusinessNextAction();
        action.setFromStatus(project.getStatus().name());
        action.setBlockedReasons(project.getRiskReason() == null ? List.of() : List.of(project.getRiskReason()));
        action.setMissingFields(List.of());
        action.setBusinessRefs(List.of("BR-008", "BR-010", "SCN-003"));
        switch (project.getStatus()) {
            case Preparing -> {
                action.setKey("project/start-preparation");
                action.setLabel("开工准备");
                action.setHint("确认项目成员、排期和开工条件，推进阶段开工。");
                action.setOwnerRole("项目经理 / 店长");
                action.setToStatus("Ongoing");
                action.setReloadScopes(List.of("project", "projectStage", "projectTask", "contract"));
            }
            case Ongoing -> {
                action.setKey("project/advance-tasks");
                action.setLabel("推进施工任务");
                action.setHint("按阶段推进任务、材料和现场汇报。");
                action.setOwnerRole("项目经理");
                action.setToStatus("Accepting");
                action.setReloadScopes(List.of("project", "projectStage", "projectTask", "materials"));
            }
            case Accepting -> {
                action.setKey("project/handle-acceptance");
                action.setLabel("处理阶段验收");
                action.setHint("提交验收或等待业主确认，驳回后回到施工整改。");
                action.setOwnerRole("项目经理 / 业主");
                action.setToStatus("Finished");
                action.setReloadScopes(List.of("acceptance", "acceptanceRound", "projectStage", "projectTask", "project"));
            }
            case Suspended -> {
                action.setKey("project/resume");
                action.setLabel("核实复工");
                action.setHint("核实停工原因，明确复工责任和日期。");
                action.setOwnerRole("项目经理 / 店长");
                action.setToStatus("Ongoing");
                action.setReloadScopes(List.of("project", "notice", "workbenchQueue"));
            }
            case Finished -> {
                action.setKey("settlement/open");
                action.setLabel("进入结算");
                action.setHint("项目已完工，进入结算与售后归档。");
                action.setOwnerRole("财务 / 项目经理");
                action.setToStatus("Settled");
                action.setReloadScopes(List.of("settlement", "project", "contract", "receivable", "payable", "cost"));
            }
            case Settled -> {
                action.setKey("project/archive");
                action.setLabel("查看归档");
                action.setHint("项目已结算，只读查看项目档案。");
                action.setOwnerRole("企业负责人 / 财务");
                action.setToStatus("Settled");
                action.setEnabled(Boolean.FALSE);
                action.setReloadScopes(List.of("project", "settlement"));
            }
        }
        return action;
    }

    private String nextOwner(ProjectStatus status) {
        return switch (status) {
            case Preparing, Ongoing, Accepting, Suspended -> "项目经理";
            case Finished, Settled -> "财务 / 项目经理";
        };
    }

    private String tone(ProjectStatus status) {
        return switch (status) {
            case Preparing -> "info";
            case Ongoing -> "alert";
            case Suspended -> "blocked";
            case Accepting -> "pending";
            case Finished -> "done";
            case Settled -> "dark";
        };
    }
}
