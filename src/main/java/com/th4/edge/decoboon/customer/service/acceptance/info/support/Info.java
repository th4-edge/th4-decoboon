package com.th4.edge.decoboon.customer.service.acceptance.info.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主验收详情实现。 */
@Note("业主验收详情")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "property", "stage", "status", "conclusion", "issues",
                        "evidenceUrls", "submittedBy", "rectificationOwner", "rectificationDueDate",
                        "rectificationType", "rectificationIssueImages", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "designer", "manager", "support"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status", "progress"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/acceptance/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.acceptance.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Long acceptanceId;

    @AutoInject
    private Long stageId;

    private final CustomerAccessContext accessContext;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectStageRepository stageRepository;

    public Info(CustomerAccessContext accessContext,
                AcceptanceRepository acceptanceRepository,
                ProjectStageRepository stageRepository) {
        this.accessContext = accessContext;
        this.acceptanceRepository = acceptanceRepository;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Acceptance call(Acceptance request) {
        Credentials credential = accessContext.requireCredential(principal);
        if (acceptanceId != null) {
            return acceptanceRepository.findForCustomerAccess(
                            acceptanceId, credential.getCreId(), CustomerAccessStatus.Active)
                    .orElseThrow(() -> new DefinedException(
                            new DefaultErrorCode("404", "验收单不存在或无权访问")));
        }
        if (stageId != null) {
            ProjectStage stage = stageRepository.findForCustomerAccess(
                            stageId, credential.getCreId(), CustomerAccessStatus.Active)
                    .orElseThrow(() -> new DefinedException(
                            new DefaultErrorCode("404", "阶段不存在或无权访问")));
            return acceptanceRepository.findFirstForCustomerByStage(stage).orElse(null);
        }
        throw new DefinedException(new DefaultErrorCode("404", "验收单不存在或无权访问"));
    }
}
