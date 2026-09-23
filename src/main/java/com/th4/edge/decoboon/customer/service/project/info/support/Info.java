package com.th4.edge.decoboon.customer.service.project.info.support;

import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.configure.StageStatus;
import java.util.List;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 业主项目详情实现。 */
@Note("业主项目详情")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "area", "status", "progress",
                        "startDate", "planEndDate", "actualEndDate", "currentStageName", "property", "customer",
                        "designer", "manager", "support"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/project/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.project.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    private final CustomerAccessContext accessContext;
    private final com.th4.edge.decoboon.project.repository.ProjectStageRepository stageRepository;
    private final com.th4.edge.decoboon.project.repository.AcceptanceRepository acceptanceRepository;

    public Info(CustomerAccessContext accessContext, com.th4.edge.decoboon.project.repository.ProjectStageRepository stageRepository, com.th4.edge.decoboon.project.repository.AcceptanceRepository acceptanceRepository) {
        this.accessContext = accessContext;
        this.stageRepository = stageRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Project call(Project request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        // 历史坑（XA-20260924-003）：`Project.currentStageName` 是需要回填的派生列，
        // 业主详情接口原先直接返回它 → 详情页"当前： · 阶段 0/8"阶段名为空，而我的家其他页正常。
        // 与 project/project/info、project/project/page、业主旅程保持同一口径：按阶段事实推导。
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        stages.stream()
                .filter(stage -> stage.getStatus() == StageStatus.Doing || stage.getStatus() == StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .ifPresent(stage -> project.setCurrentStageName(stage.getName()));

        // 项目状态同理：业主详情原先直出可能漂移的 status 列，导致"详情待验收 / 首页与旅程施工中"。
        // 与 customer/project/page、业主旅程同口径：存在 Pending 验收单 ⇔ 待验收；否则已开工 ⇒ 施工中。
        com.th4.edge.decoboon.project.configure.ProjectStatus current =
                project.getStatus() == null
                        ? com.th4.edge.decoboon.project.configure.ProjectStatus.Preparing : project.getStatus();
        if (current != com.th4.edge.decoboon.project.configure.ProjectStatus.Finished
                && current != com.th4.edge.decoboon.project.configure.ProjectStatus.Settled
                && current != com.th4.edge.decoboon.project.configure.ProjectStatus.Suspended) {
            boolean pendingAcceptance = acceptanceRepository
                    .findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId()).stream()
                    .anyMatch(item -> item.getStatus() == com.th4.edge.decoboon.project.configure.AcceptStatus.Pending);
            if (pendingAcceptance) {
                project.setStatus(com.th4.edge.decoboon.project.configure.ProjectStatus.Accepting);
            } else {
                boolean anyStarted = stages.stream().anyMatch(stage -> stage.getStatus() == StageStatus.Doing
                        || stage.getStatus() == StageStatus.Done);
                boolean allDone = !stages.isEmpty() && stages.stream()
                        .allMatch(stage -> stage.getStatus() == StageStatus.Done);
                project.setStatus(allDone
                        ? com.th4.edge.decoboon.project.configure.ProjectStatus.Finished
                        : anyStarted
                                ? com.th4.edge.decoboon.project.configure.ProjectStatus.Ongoing
                                : com.th4.edge.decoboon.project.configure.ProjectStatus.Preparing);
            }
        }
        return project;
    }
}
