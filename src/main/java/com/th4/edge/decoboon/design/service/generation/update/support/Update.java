package com.th4.edge.decoboon.design.service.generation.update.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignGenerationConfirmationContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignRenderVersion;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.repository.DesignRenderVersionRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/** 设计师确认AI效果图版本实现。 */
@Note("AI效果图确认")
@Properties(allow = {
        @AllowProperties(model = DesignGenerationTask.class,
                properties = {"generationId", "no", "design", "customer", "property", "owner", "mode", "status",
                        "version", "failureCode", "builtAt",
                        "completedAt", "selectedRender", "renderVersions", "createTime", "updateTime"}),
        @AllowProperties(model = DesignRenderVersion.class,
                properties = {"renderId", "sequence", "status", "imageUrl", "deliverable", "confirmedBy", "confirmedAt", "startedAt", "completedAt"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "coverUrl", "updateTime"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "community", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/generation/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.design.service.generation.update.Update {

    @AutoInject
    @NotNull
    private Long generationId;

    @AutoInject
    @NotNull
    private Long renderId;

    @AutoInject
    private Long spaceId;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignGenerationTaskRepository taskRepository;
    private final DesignRenderVersionRepository renderVersionRepository;
    private final DesignGenerationConfirmationContext confirmationContext;
    private final DataScopeContext dataScopeContext;

    public Update(DesignContext designContext,
                  DesignGenerationTaskRepository taskRepository,
                  DesignRenderVersionRepository renderVersionRepository,
                  DesignGenerationConfirmationContext confirmationContext,
                  DataScopeContext dataScopeContext) {
        this.designContext = designContext;
        this.taskRepository = taskRepository;
        this.renderVersionRepository = renderVersionRepository;
        this.confirmationContext = confirmationContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public DesignGenerationTask call(DesignGenerationTask request) {
        Employee current = designContext.requireDesignWriteAccess(employee);
        DataScope scope = dataScopeContext.requireScope(current);
        Specification<DesignGenerationTask> byId = (root, query, builder) ->
                builder.equal(root.get("generationId"), generationId);
        DesignGenerationTask task = taskRepository.findOne(
                        byId.and(DataScopeSpecification.visible(DesignGenerationTask.class, scope)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "AI生成任务不存在或无权访问")));
        DesignGenerationTask confirmed = confirmationContext.confirm(task, renderId, spaceId, current);
        confirmed.setRenderVersions(renderVersionRepository
                .findByTask_GenerationIdOrderBySequenceAsc(generationId));
        return confirmed;
    }
}
