package com.th4.edge.decoboon.customer.service.project.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 仅返回当前微信身份显式授权的项目。 */
@Note("业主项目分页")
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
@Service("customer/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.project.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;
    private final com.th4.edge.decoboon.project.repository.ProjectStageRepository stageRepository;
    private final com.th4.edge.decoboon.project.repository.AcceptanceRepository acceptanceRepository;

    public Page(CustomerAccessContext accessContext,
                com.th4.edge.decoboon.project.repository.ProjectStageRepository stageRepository,
                com.th4.edge.decoboon.project.repository.AcceptanceRepository acceptanceRepository) {
        this.accessContext = accessContext;
        this.stageRepository = stageRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Project> call(Project request) {
        Credentials credential = accessContext.requireCredential(principal);
        List<Project> projects = accessContext.activeAccesses(credential).stream()
                .map(access -> access.getProject())
                .distinct()
                .sorted(Comparator.comparing(Project::getProjectId).reversed())
                .peek(this::applyOwnerProjection)
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, projects.size());
        int to = Math.min(from + pageSize, projects.size());
        return new PageImpl<>(projects.subList(from, to), PageRequest.of(page, pageSize), projects.size());
    }

    /**
     * 业主可见项目投影（XA-20260924-009）。
     *
     * <p>历史坑：本投影原先直接返回实体，`Project.status` / `currentStageName` 都是可能漂移的派生列，
     * 导致同一个业主在「我的家」首页看到"待验收"、在项目详情/项目旅程看到"施工中"、当前阶段一边空白一边有值。
     * 这里与 `customer/project/info`、业主旅程保持同一口径：</p>
     * <ul>
     *   <li>当前阶段：Doing/Accepting 优先，否则第一个阶段；</li>
     *   <li>项目状态：存在 Pending 验收单 ⇔ 待验收；否则已开工 ⇒ 施工中；停工与终态保留。</li>
     * </ul>
     */
    private void applyOwnerProjection(Project project) {
        List<com.th4.edge.decoboon.project.model.ProjectStage> stages =
                stageRepository.findByProjectOrderBySort(project);
        stages.stream()
                .filter(stage -> stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Doing
                        || stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .ifPresent(stage -> project.setCurrentStageName(stage.getName()));

        com.th4.edge.decoboon.project.configure.ProjectStatus current =
                project.getStatus() == null
                        ? com.th4.edge.decoboon.project.configure.ProjectStatus.Preparing : project.getStatus();
        if (current == com.th4.edge.decoboon.project.configure.ProjectStatus.Finished
                || current == com.th4.edge.decoboon.project.configure.ProjectStatus.Settled
                || current == com.th4.edge.decoboon.project.configure.ProjectStatus.Suspended) {
            return;
        }
        boolean pendingAcceptance = acceptanceRepository
                .findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId()).stream()
                .anyMatch(item -> item.getStatus() == com.th4.edge.decoboon.project.configure.AcceptStatus.Pending);
        if (pendingAcceptance) {
            project.setStatus(com.th4.edge.decoboon.project.configure.ProjectStatus.Accepting);
            return;
        }
        boolean anyStarted = stages.stream().anyMatch(stage ->
                stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Doing
                        || stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Done);
        boolean allDone = !stages.isEmpty() && stages.stream()
                .allMatch(stage -> stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Done);
        project.setStatus(allDone
                ? com.th4.edge.decoboon.project.configure.ProjectStatus.Finished
                : anyStarted
                        ? com.th4.edge.decoboon.project.configure.ProjectStatus.Ongoing
                        : com.th4.edge.decoboon.project.configure.ProjectStatus.Preparing);
    }
}
