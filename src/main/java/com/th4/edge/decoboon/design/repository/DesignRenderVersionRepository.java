package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignRenderVersion;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** AI效果图版本仓储。 */
public interface DesignRenderVersionRepository extends JpaRepository<DesignRenderVersion, Long> {

    List<DesignRenderVersion> findByTask_GenerationIdOrderBySequenceAsc(Long generationId);

    Optional<DesignRenderVersion> findByRenderIdAndTask_GenerationId(Long renderId, Long generationId);

    long countByTask_GenerationId(Long generationId);
}
