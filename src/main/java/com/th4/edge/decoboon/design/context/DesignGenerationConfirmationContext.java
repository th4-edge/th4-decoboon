package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.decoboon.design.configure.DesignRenderStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.model.DesignRenderVersion;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.DesignRenderVersionRepository;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 设计师确认效果图并回写设计。 */
@Note("AI效果图确认上下文")
@Component
public class DesignGenerationConfirmationContext {

    private final DesignGenerationTaskRepository taskRepository;
    private final DesignRenderVersionRepository renderVersionRepository;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;

    public DesignGenerationConfirmationContext(DesignGenerationTaskRepository taskRepository,
                                               DesignRenderVersionRepository renderVersionRepository,
                                               DesignRepository designRepository,
                                               SpaceRepository spaceRepository) {
        this.taskRepository = taskRepository;
        this.renderVersionRepository = renderVersionRepository;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
    }

    @Transactional
    public DesignGenerationTask confirm(DesignGenerationTask task, Long renderId, Long spaceId, Employee owner) {
        if (task == null || task.getGenerationId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "AI生成任务不存在"));
        }
        if (task.getStatus() == DesignGenerationStatus.Locked
                || task.getStatus() == DesignGenerationStatus.Shared) {
            throw new DefinedException(new DefaultErrorCode("409", "当前AI生成任务已进入不可回退阶段"));
        }
        if (task.getStatus() != DesignGenerationStatus.DraftReady
                && task.getStatus() != DesignGenerationStatus.DesignerConfirmed) {
            throw new DefinedException(new DefaultErrorCode("409", "AI效果图尚未生成完成"));
        }
        DesignRenderVersion selected = renderVersionRepository
                .findByRenderIdAndTask_GenerationId(renderId, task.getGenerationId())
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "AI效果图版本不存在或无权访问")));
        if (selected.getImageUrl() == null || selected.getImageUrl().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("409", "AI效果图版本没有可用图片"));
        }

        List<DesignRenderVersion> versions = renderVersionRepository
                .findByTask_GenerationIdOrderBySequenceAsc(task.getGenerationId());
        for (DesignRenderVersion version : versions) {
            if (version.getRenderId().equals(renderId)) {
                version.setStatus(DesignRenderStatus.Confirmed);
                version.setDeliverable(Boolean.TRUE);
                version.setConfirmedBy(owner);
                version.setConfirmedAt(LocalDateTime.now());
            } else if (version.getStatus() != DesignRenderStatus.Failed) {
                version.setStatus(DesignRenderStatus.Superseded);
                version.setDeliverable(Boolean.FALSE);
            }
        }
        renderVersionRepository.saveAll(versions);

        Design design = task.getDesign();
        if (spaceId == null) {
            design.setCoverUrl(selected.getImageUrl());
            designRepository.save(design);
        } else {
            Space space = spaceRepository.findBySpaceIdAndDesign_DesignId(spaceId, design.getDesignId())
                    .orElseThrow(() -> new DefinedException(
                            new DefaultErrorCode("404", "设计区域不存在或不属于当前方案")));
            List<String> images = space.getImages() == null
                    ? new ArrayList<>() : new ArrayList<>(space.getImages());
            if (!images.contains(selected.getImageUrl())) {
                images.add(selected.getImageUrl());
            }
            space.setImages(images);
            spaceRepository.save(space);
        }

        task.setSelectedRender(selected);
        task.setStatus(DesignGenerationStatus.DesignerConfirmed);
        taskRepository.save(task);
        return task;
    }
}
