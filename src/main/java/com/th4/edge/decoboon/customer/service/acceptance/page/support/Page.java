package com.th4.edge.decoboon.customer.service.acceptance.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 业主验收分页实现。 */
@Note("业主验收分页")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "property", "stage", "status", "conclusion", "issues",
                        "evidenceUrls", "rectificationOwner", "rectificationDueDate", "rectificationType",
                        "rectificationIssueImages", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status", "progress"})
})
@Service("customer/acceptance/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.acceptance.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;
    private final AcceptanceRepository acceptanceRepository;

    public Page(CustomerAccessContext accessContext, AcceptanceRepository acceptanceRepository) {
        this.accessContext = accessContext;
        this.acceptanceRepository = acceptanceRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Acceptance> call(Acceptance request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        var all = acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId());
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(page, pageSize), all.size());
    }
}
