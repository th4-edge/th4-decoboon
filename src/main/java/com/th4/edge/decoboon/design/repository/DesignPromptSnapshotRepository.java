package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignPromptSnapshot;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** AI设计Prompt快照仓储。 */
public interface DesignPromptSnapshotRepository extends JpaRepository<DesignPromptSnapshot, Long> {

    Optional<DesignPromptSnapshot> findFirstByTask_GenerationIdOrderByPromptIdDesc(Long generationId);

    List<DesignPromptSnapshot> findByTask_GenerationIdOrderByPromptIdAsc(Long generationId);
}
