package com.th4.edge.decoboon.project.service.acceptances.info.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 阶段验收详情实现。 */
@Note("阶段验收详情")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "stage", "submittedBy", "status", "conclusion",
                        "issues", "evidenceUrls", "rectificationOwner", "rectificationDueDate", "rectificationType",
                        "rectificationIssueImages", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "property", "designer", "manager", "support" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/acceptance/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.acceptances.info.Info {

    @AutoInject
    @NotNull
    private Long stageId;

    @User
    private Employee employee;

    private final ProjectStageRepository stageRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectAccessContext accessContext;

    public Info(ProjectStageRepository stageRepository,
                AcceptanceRepository acceptanceRepository,
                ProjectAccessContext accessContext) {
        this.stageRepository = stageRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.accessContext = accessContext;
    }

    @Override
    public Acceptance call(Acceptance request) {
        Specification<com.th4.edge.decoboon.project.model.ProjectStage> byId = (root, query, builder) ->
                builder.equal(root.get("stageId"), stageId);
        var stage = stageRepository.findOne(byId.and(accessContext.visibleChild(
                        employee, com.th4.edge.decoboon.project.model.ProjectStage.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在或无权访问")));
        return acceptanceRepository.findFirstByStageOrderByAcceptanceIdDesc(stage).orElse(null);
    }
}
