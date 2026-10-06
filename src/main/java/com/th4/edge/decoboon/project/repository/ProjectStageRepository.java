package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 项目阶段仓储。
 */
public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long>,
        JpaSpecificationExecutor<ProjectStage> {

    List<ProjectStage> findByProjectOrderBySort(Project project);

    /** 非加锁读取；阶段状态推进的并发由 DistributedLock / @DistributedLocked 串行化。 */
    Optional<ProjectStage> findByStageId(Long stageId);

    @Query("""
            select stage from ProjectStage stage
            where stage.stageId = :stageId
              and exists (
                select access.accessId from CustomerAccess access
                where access.project = stage.project
                  and access.credential.creId = :creId
                  and access.status = :status
              )
            """)
    Optional<ProjectStage> findForCustomerAccess(
            @Param("stageId") Long stageId,
            @Param("creId") Long creId,
            @Param("status") com.th4.edge.decoboon.customer.configure.CustomerAccessStatus status);
}
