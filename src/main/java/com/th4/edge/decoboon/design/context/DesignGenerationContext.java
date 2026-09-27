package com.th4.edge.decoboon.design.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.design.configure.DesignGenerationMode;
import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignContextSnapshot;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.decoboon.design.repository.DesignContextSnapshotRepository;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.repository.DesignPromptSnapshotRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

/** AI设计上下文、Prompt和任务的事务编排。 */
@Note("AI设计生成上下文")
@Component
public class DesignGenerationContext {

    private final DesignAiContextBuilder contextBuilder;
    private final DesignPromptRenderer promptRenderer;
    private final DesignGenerationTaskRepository taskRepository;
    private final DesignContextSnapshotRepository contextSnapshotRepository;
    private final DesignPromptSnapshotRepository promptSnapshotRepository;
    private final ObjectMapper objectMapper;

    public DesignGenerationContext(DesignAiContextBuilder contextBuilder,
                                   DesignPromptRenderer promptRenderer,
                                   DesignGenerationTaskRepository taskRepository,
                                   DesignContextSnapshotRepository contextSnapshotRepository,
                                   DesignPromptSnapshotRepository promptSnapshotRepository,
                                   ObjectMapper objectMapper) {
        this.contextBuilder = contextBuilder;
        this.promptRenderer = promptRenderer;
        this.taskRepository = taskRepository;
        this.contextSnapshotRepository = contextSnapshotRepository;
        this.promptSnapshotRepository = promptSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DesignGenerationTask build(DesignGenerationTask request, Design design,
                                      Employee owner, DesignGenerationMode requestedMode) {
        DesignAiContextBuilder.ContextData context = contextBuilder.build(design,
                requestedMode == null ? DesignGenerationMode.Precise : requestedMode);
        DesignPromptRenderer.PromptBundle prompt = promptRenderer.render(context);
        var existing = taskRepository
                .findFirstByCompany_ComIdAndDesign_DesignIdAndSourceDigestAndStatusInOrderByGenerationIdDesc(
                        owner.getCompany().getComId(), design.getDesignId(), context.getSourceDigest(),
                        EnumSet.of(DesignGenerationStatus.ContextBuilding, DesignGenerationStatus.PromptReady,
                                DesignGenerationStatus.Generating, DesignGenerationStatus.DraftReady));
        if (existing.isPresent()) {
            DesignGenerationTask duplicate = existing.get();
            duplicate.setContextSnapshot(contextSnapshotRepository
                    .findFirstByTask_GenerationIdOrderBySnapshotIdDesc(duplicate.getGenerationId()).orElse(null));
            duplicate.setPromptSnapshot(promptSnapshotRepository
                    .findFirstByTask_GenerationIdOrderByPromptIdDesc(duplicate.getGenerationId()).orElse(null));
            return duplicate;
        }

        DesignGenerationTask task = new DesignGenerationTask();
        task.setNo("G" + design.getDesignId() + "-" + System.currentTimeMillis());
        apply(task, design, owner);
        task.setMode(context.getMode());
        task.setStatus(DesignGenerationStatus.PromptReady);
        task.setVersion(1);
        task.setSourceDigest(context.getSourceDigest());
        task.setModelName(prompt.getModelName());
        task.setBuiltAt(LocalDateTime.now());
        task = taskRepository.save(task);

        DesignContextSnapshot contextSnapshot = new DesignContextSnapshot();
        apply(contextSnapshot, task, design, owner);
        contextSnapshot.setContextJson(context.getContextJson());
        contextSnapshot.setSourceDigest(context.getSourceDigest());
        contextSnapshot.setMissingFieldsJson(json(context.getMissingFields()));
        contextSnapshot.setConflictFlagsJson(json(context.getConflictFlags()));
        contextSnapshot.setConfidence(context.getConfidence());
        contextSnapshot.setCapturedAt(LocalDateTime.now());
        contextSnapshot = contextSnapshotRepository.save(contextSnapshot);

        DesignPromptSnapshot promptSnapshot = new DesignPromptSnapshot();
        apply(promptSnapshot, task, contextSnapshot, design, owner);
        promptSnapshot.setVisualPrompt(prompt.getVisualPrompt());
        promptSnapshot.setTalkPrompt(prompt.getTalkPrompt());
        promptSnapshot.setDocumentBriefJson(prompt.getDocumentBriefJson());
        promptSnapshot.setTemplateVersion(prompt.getTemplateVersion());
        promptSnapshot.setModelName(prompt.getModelName());
        promptSnapshot.setModelParametersJson(prompt.getModelParametersJson());
        promptSnapshot.setRenderedAt(LocalDateTime.now());
        promptSnapshot = promptSnapshotRepository.save(promptSnapshot);

        task.setContextSnapshot(contextSnapshot);
        task.setPromptSnapshot(promptSnapshot);
        return task;
    }

    private void apply(DesignGenerationTask task, Design design, Employee owner) {
        task.setCompany(owner.getCompany());
        task.setStore(owner.getStore());
        task.setTeam(owner.getTeam());
        task.setOwner(owner);
        task.setCustomer(design.getCustomer());
        task.setProperty(design.getProperty());
        task.setDesign(design);
    }

    private void apply(DesignContextSnapshot snapshot, DesignGenerationTask task,
                       Design design, Employee owner) {
        snapshot.setCompany(owner.getCompany());
        snapshot.setStore(owner.getStore());
        snapshot.setTeam(owner.getTeam());
        snapshot.setOwner(owner);
        snapshot.setCustomer(design.getCustomer());
        snapshot.setProperty(design.getProperty());
        snapshot.setDesign(design);
        snapshot.setTask(task);
    }

    private void apply(DesignPromptSnapshot snapshot, DesignGenerationTask task,
                       DesignContextSnapshot contextSnapshot, Design design, Employee owner) {
        snapshot.setCompany(owner.getCompany());
        snapshot.setStore(owner.getStore());
        snapshot.setTeam(owner.getTeam());
        snapshot.setOwner(owner);
        snapshot.setCustomer(design.getCustomer());
        snapshot.setProperty(design.getProperty());
        snapshot.setDesign(design);
        snapshot.setTask(task);
        snapshot.setContextSnapshot(contextSnapshot);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "AI设计快照序列化失败"));
        }
    }
}
