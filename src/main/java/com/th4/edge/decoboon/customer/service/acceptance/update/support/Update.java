package com.th4.edge.decoboon.customer.service.acceptance.update.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主验收确认实现，重复提交同一结果时保持幂等。 */
@Note("业主验收确认")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "property", "stage", "status", "conclusion", "issues",
                        "evidenceUrls", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status", "progress"})
})
@Service("customer/acceptance/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.customer.service.acceptance.update.Update {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long acceptanceId;

    @AutoInject
    @NotNull
    private Boolean confirmed;

    @AutoInject
    @Length(max = 1024)
    private String conclusion;

    private final CustomerAccessContext accessContext;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectContext projectContext;

    public Update(CustomerAccessContext accessContext,
                  AcceptanceRepository acceptanceRepository,
                  ProjectContext projectContext) {
        this.accessContext = accessContext;
        this.acceptanceRepository = acceptanceRepository;
        this.projectContext = projectContext;
    }

    @Override
    @Transactional
    public Acceptance call(Acceptance request) {
        Credentials credential = accessContext.requireCredential(principal);
        Acceptance acceptance = acceptanceRepository.findLockedForCustomerAccess(
                        acceptanceId, credential.getCreId(), CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "验收单不存在或无权访问")));
        AcceptStatus target = Boolean.TRUE.equals(confirmed) ? AcceptStatus.Confirmed : AcceptStatus.Rejected;
        if (acceptance.getStatus() != AcceptStatus.Pending) {
            if (acceptance.getStatus() == target) {
                return acceptance;
            }
            throw new DefinedException(new DefaultErrorCode("400", "验收单已处理"));
        }
        return projectContext.confirmAcceptance(acceptance, credential, confirmed, conclusion);
    }
}
