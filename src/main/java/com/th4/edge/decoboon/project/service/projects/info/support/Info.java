package com.th4.edge.decoboon.project.service.projects.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/** 项目详情实现。 */
@Note("项目详情")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "quote", "designer", "manager",
                        "siteAddr", "area", "contractAmount", "cost", "status", "progress", "startDate",
                        "planEndDate", "actualEndDate", "currentStageName", "createTime" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status", "owner"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "createdBy", "amount", "cost", "discount", "status", "property"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role"})
})
@Service("project/project/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.projects.info.Info {

    @AutoInject
    @NotNull
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectAccessContext accessContext;

    public Info(ProjectRepository projectRepository,
                ProjectStageRepository stageRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.accessContext = accessContext;
    }

    @Override
    public Project call(Project request) {
        Project project = accessContext.requireProject(projectId, employee);
        var stages = stageRepository.findByProjectOrderBySort(project);
        stages.stream()
                .filter(stage -> stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Doing
                        || stage.getStatus() == com.th4.edge.decoboon.project.configure.StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .ifPresent(stage -> project.setCurrentStageName(stage.getName()));
        return project;
    }
}
