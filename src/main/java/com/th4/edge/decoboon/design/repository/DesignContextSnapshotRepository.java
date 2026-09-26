package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignContextSnapshot;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** AI设计上下文快照仓储。 */
public interface DesignContextSnapshotRepository extends JpaRepository<DesignContextSnapshot, Long> {

    Optional<DesignContextSnapshot> findFirstByTask_GenerationIdOrderBySnapshotIdDesc(Long generationId);

    List<DesignContextSnapshot> findByTask_GenerationIdOrderBySnapshotIdAsc(Long generationId);
}
