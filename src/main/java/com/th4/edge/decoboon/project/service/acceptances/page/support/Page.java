package com.th4.edge.decoboon.project.service.acceptances.page.support;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 验收单分页实现。 */
@Note("验收单分页")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "stage", "submittedBy", "status", "conclusion",
                        "issues", "evidenceUrls", "rectificationOwner", "rectificationDueDate", "rectificationType",
                        "rectificationIssueImages", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/acceptance/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.acceptances.page.Page {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                AcceptanceRepository acceptanceRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Acceptance> call(Acceptance request) {
        if (projectId == null) return new PageImpl<>(java.util.List.of());
        var project = accessContext.requireProject(projectId, employee);
        return acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(projectId,
                org.springframework.data.domain.PageRequest.of(0, 100));
    }
}
