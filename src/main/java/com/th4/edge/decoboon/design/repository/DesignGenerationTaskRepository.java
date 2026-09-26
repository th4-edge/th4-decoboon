package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** AI设计生成任务仓储。 */
public interface DesignGenerationTaskRepository extends JpaRepository<DesignGenerationTask, Long>,
        JpaSpecificationExecutor<DesignGenerationTask> {

    Optional<DesignGenerationTask> findByGenerationIdAndCompany_ComId(Long generationId, Long comId);

    List<DesignGenerationTask> findByCompany_ComIdAndDesign_DesignIdOrderByGenerationIdDesc(Long comId, Long designId);

    Optional<DesignGenerationTask> findFirstByCompany_ComIdAndDesign_DesignIdAndSourceDigestAndStatusInOrderByGenerationIdDesc(
            Long comId, Long designId, String sourceDigest, Collection<DesignGenerationStatus> statuses);
}
