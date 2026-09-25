package com.th4.edge.decoboon.design.service.generation.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignContextSnapshot;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.decoboon.design.model.DesignRenderVersion;
import com.th4.edge.decoboon.design.repository.DesignContextSnapshotRepository;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.repository.DesignPromptSnapshotRepository;
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

/** AI设计生成任务详情实现。 */
@Note("AI设计生成任务详情")
@Properties(allow = {
        @AllowProperties(model = DesignGenerationTask.class,
                properties = {"generationId", "no", "design", "customer", "property", "owner", "mode", "status",
                        "sourceDigest", "version", "modelName", "failureCode", "failureMessage", "builtAt",
                        "completedAt", "contextSnapshot", "promptSnapshot", "renderVersions", "createTime", "updateTime"}),
        @AllowProperties(model = DesignContextSnapshot.class,
                properties = {"snapshotId", "contextJson", "sourceDigest", "missingFieldsJson", "conflictFlagsJson",
                        "confidence", "capturedAt"}),
        @AllowProperties(model = DesignPromptSnapshot.class,
                properties = {"promptId", "visualPrompt", "talkPrompt", "documentBriefJson", "templateVersion",
                        "modelName", "modelParametersJson", "renderedAt"}),
        @AllowProperties(model = DesignRenderVersion.class,
                properties = {"renderId", "sequence", "status", "imageUrl", "modelName", "requestId",
                        "inputImageCount", "outputImageCount", "usageJson", "costAmount", "costCurrency",
                        "startedAt", "completedAt", "createTime"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "cadUrl",
                        "cadPreviewUrl", "originalPlanUrl", "updateTime"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "community", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/generation/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.design.service.generation.info.Info {

    @AutoInject
    @NotNull
    private Long generationId;

    @User
    private Employee employee;

    private final DesignGenerationTaskRepository taskRepository;
    private final DesignContextSnapshotRepository contextSnapshotRepository;
    private final DesignPromptSnapshotRepository promptSnapshotRepository;
    private final DesignRenderVersionRepository renderVersionRepository;
    private final DataScopeContext dataScopeContext;

    public Info(DesignGenerationTaskRepository taskRepository,
                DesignContextSnapshotRepository contextSnapshotRepository,
                DesignPromptSnapshotRepository promptSnapshotRepository,
                DesignRenderVersionRepository renderVersionRepository,
                DataScopeContext dataScopeContext) {
        this.taskRepository = taskRepository;
        this.contextSnapshotRepository = contextSnapshotRepository;
        this.promptSnapshotRepository = promptSnapshotRepository;
        this.renderVersionRepository = renderVersionRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public DesignGenerationTask call(DesignGenerationTask request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<DesignGenerationTask> byId = (root, query, builder) ->
                builder.equal(root.get("generationId"), generationId);
        DesignGenerationTask task = taskRepository.findOne(
                        byId.and(DataScopeSpecification.visible(DesignGenerationTask.class, scope)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "AI生成任务不存在或无权访问")));
        task.setContextSnapshot(contextSnapshotRepository
                .findFirstByTask_GenerationIdOrderBySnapshotIdDesc(generationId).orElse(null));
        task.setPromptSnapshot(promptSnapshotRepository
                .findFirstByTask_GenerationIdOrderByPromptIdDesc(generationId).orElse(null));
        List<DesignRenderVersion> versions = renderVersionRepository
                .findByTask_GenerationIdOrderBySequenceAsc(generationId);
        task.setRenderVersions(versions);
        return task;
    }
}
