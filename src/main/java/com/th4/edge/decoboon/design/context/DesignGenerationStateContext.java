package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.decoboon.design.configure.DesignRenderStatus;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.decoboon.design.model.DesignRenderVersion;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.repository.DesignPromptSnapshotRepository;
import com.th4.edge.decoboon.design.repository.DesignRenderVersionRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** AI生成任务的短事务状态读写，外部模型调用必须发生在事务外。 */
@Note("AI生成任务状态上下文")
@Component
public class DesignGenerationStateContext {

    private final DesignGenerationTaskRepository taskRepository;
    private final DesignPromptSnapshotRepository promptSnapshotRepository;
    private final DesignRenderVersionRepository renderVersionRepository;
    private final DesignReferenceImageContext referenceImageContext;

    public DesignGenerationStateContext(DesignGenerationTaskRepository taskRepository,
                                        DesignPromptSnapshotRepository promptSnapshotRepository,
                                        DesignRenderVersionRepository renderVersionRepository,
                                        DesignReferenceImageContext referenceImageContext) {
        this.taskRepository = taskRepository;
        this.promptSnapshotRepository = promptSnapshotRepository;
        this.renderVersionRepository = renderVersionRepository;
        this.referenceImageContext = referenceImageContext;
    }

    @Transactional(readOnly = true)
    public RunInput start(Long generationId) {
        DesignGenerationTask task = requireTask(generationId);
        if (task.getStatus() != DesignGenerationStatus.PromptReady) {
            return null;
        }
        DesignPromptSnapshot prompt = promptSnapshotRepository
                .findFirstByTask_GenerationIdOrderByPromptIdDesc(generationId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("409", "AI生成任务缺少Prompt快照")));
        String referencePath = referenceImageContext.firstImagePath(task.getDesign());
        if (referencePath == null || referencePath.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少可用于AI生成的CAD图片或户型图片"));
        }
        return new RunInput(task.getGenerationId(), prompt.getVisualPrompt(), referencePath,
                prompt.getModelName(), prompt.getModelParametersJson());
    }

    @Transactional
    public void markGenerating(Long generationId) {
        DesignGenerationTask task = requireTask(generationId);
        if (task.getStatus() != DesignGenerationStatus.PromptReady) {
            return;
        }
        task.setStatus(DesignGenerationStatus.Generating);
        task.setFailureCode(null);
        task.setFailureMessage(null);
        taskRepository.save(task);
    }

    @Transactional
    public DesignRenderVersion complete(Long generationId, DesignRenderClient.RenderResult result) {
        DesignGenerationTask task = requireTask(generationId);
        DesignPromptSnapshot prompt = promptSnapshotRepository
                .findFirstByTask_GenerationIdOrderByPromptIdDesc(generationId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("409", "AI生成任务缺少Prompt快照")));
        int sequence = Math.toIntExact(renderVersionRepository.countByTask_GenerationId(generationId) + 1);
        DesignRenderVersion version = new DesignRenderVersion();
        version.setCompany(task.getCompany());
        version.setStore(task.getStore());
        version.setTeam(task.getTeam());
        version.setOwner(task.getOwner());
        version.setCustomer(task.getCustomer());
        version.setProperty(task.getProperty());
        version.setDesign(task.getDesign());
        version.setTask(task);
        version.setPromptSnapshot(prompt);
        version.setSequence(sequence);
        version.setStatus(DesignRenderStatus.Ready);
        version.setImageUrl(result.getImageUrl());
        version.setModelName(prompt.getModelName());
        version.setRequestId(result.getRequestId());
        version.setInputImageCount(result.getInputImageCount());
        version.setOutputImageCount(result.getOutputImageCount());
        version.setUsageJson(result.getUsageJson());
        version.setStartedAt(task.getBuiltAt());
        version.setCompletedAt(LocalDateTime.now());
        version = renderVersionRepository.save(version);

        task.setStatus(DesignGenerationStatus.DraftReady);
        task.setCompletedAt(version.getCompletedAt());
        taskRepository.save(task);
        return version;
    }

    @Transactional
    public void fail(Long generationId, String code, String message) {
        DesignGenerationTask task = requireTask(generationId);
        task.setStatus(DesignGenerationStatus.Failed);
        task.setFailureCode(code);
        task.setFailureMessage(abbreviate(message));
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private DesignGenerationTask requireTask(Long generationId) {
        return taskRepository.findById(generationId)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "AI生成任务不存在")));
    }

    private String abbreviate(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    /** 外部调用所需的不可变输入。 */
    public static final class RunInput {
        private final Long generationId;
        private final String visualPrompt;
        private final String referenceImagePath;
        private final String modelName;
        private final String modelParametersJson;

        private RunInput(Long generationId, String visualPrompt, String referenceImagePath,
                         String modelName, String modelParametersJson) {
            this.generationId = generationId;
            this.visualPrompt = visualPrompt;
            this.referenceImagePath = referenceImagePath;
            this.modelName = modelName;
            this.modelParametersJson = modelParametersJson;
        }

        public Long getGenerationId() { return generationId; }
        public String getVisualPrompt() { return visualPrompt; }
        public String getReferenceImagePath() { return referenceImagePath; }
        public String getModelName() { return modelName; }
        public String getModelParametersJson() { return modelParametersJson; }
    }
}
