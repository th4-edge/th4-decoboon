package com.th4.edge.decoboon.design.service.generation.build.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignGenerationMode;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignGenerationContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignContextSnapshot;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** AI设计生成任务构建实现。 */
@Note("AI设计生成任务构建")
@Properties(allow = {
        @AllowProperties(model = DesignGenerationTask.class,
                properties = {"generationId", "no", "design", "customer", "property", "owner", "mode", "status",
                        "sourceDigest", "version", "modelName", "failureCode", "failureMessage", "builtAt", "completedAt",
                        "contextSnapshot", "promptSnapshot", "createTime", "updateTime"}),
        @AllowProperties(model = DesignContextSnapshot.class,
                properties = {"snapshotId", "contextJson", "sourceDigest", "missingFieldsJson", "conflictFlagsJson",
                        "confidence", "capturedAt"}),
        @AllowProperties(model = DesignPromptSnapshot.class,
                properties = {"promptId", "visualPrompt", "talkPrompt", "documentBriefJson", "templateVersion",
                        "modelName", "modelParametersJson", "renderedAt"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "cadUrl",
                        "cadPreviewUrl", "originalPlanUrl", "updateTime"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "community", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/generation/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.design.service.generation.build.Build {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    private DesignGenerationMode mode;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignGenerationContext generationContext;

    public Build(DesignContext designContext, DesignGenerationContext generationContext) {
        this.designContext = designContext;
        this.generationContext = generationContext;
    }

    @Override
    public DesignGenerationTask call(DesignGenerationTask request) {
        Employee current = designContext.requireDesignWriteAccess(employee);
        Design design = designContext.requireDesign(designId, current);
        return generationContext.build(request, design, current, mode);
    }
}
