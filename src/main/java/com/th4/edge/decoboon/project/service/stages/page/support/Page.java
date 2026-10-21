package com.th4.edge.decoboon.project.service.stages.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 阶段分页实现。 */
@Note("阶段分页")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "weight", "status",
                        "progress", "baselineProgress", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime",
                        "notApplicableReason", "notApplicableBy", "notApplicableAt"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "name", "siteAddr"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/stage/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.stages.page.Page {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                ProjectStageRepository stageRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProjectStage> call(ProjectStage request) {
        var project = accessContext.requireReadableProject(projectId, employee);
        return new PageImpl<>(stageRepository.findDetailedByProjectOrderBySort(project));
    }
}
