package com.th4.edge.decoboon.customer.service.stage.info.support;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主阶段详情实现。 */
@Note("业主阶段详情")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "property", "name", "stageCode", "sort", "weight", "status", "progress",
                        "plannedStart", "plannedEnd", "actualStart", "actualEnd",
                        "notApplicableReason", "notApplicableBy", "notApplicableAt"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "roomNo", "address"})
})
@Service("customer/stage/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.stage.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;

    @AutoInject
    @NotNull
    private Long stageId;

    private final CustomerAccessContext accessContext;
    private final ProjectStageRepository stageRepository;

    public Info(CustomerAccessContext accessContext, ProjectStageRepository stageRepository) {
        this.accessContext = accessContext;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectStage call(ProjectStage request) {
        Credentials credential = accessContext.requireCredential(principal);
        return stageRepository.findForCustomerAccess(
                        stageId, credential.getCreId(), CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "阶段不存在或无权访问")));
    }
}
