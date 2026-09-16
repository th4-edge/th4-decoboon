package com.th4.edge.decoboon.customer.service.stage.list.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 业主项目阶段列表实现。 */
@Note("业主项目阶段列表")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "property", "name", "sort", "weight", "status", "progress",
                        "plannedStart", "plannedEnd", "actualStart", "actualEnd"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "roomNo", "address"})
})
@Service("customer/stage/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.customer.service.stage.list.List {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    private final CustomerAccessContext accessContext;
    private final ProjectStageRepository stageRepository;

    public List(CustomerAccessContext accessContext, ProjectStageRepository stageRepository) {
        this.accessContext = accessContext;
        this.stageRepository = stageRepository;
    }

    @Override
    public java.util.List<ProjectStage> call(ProjectStage request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        return stageRepository.findByProjectOrderBySort(project);
    }
}
