package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * 行级排他锁定阶段，用于开工等状态推进的并发串行化：
     * 并发请求在锁上排队，后到者读到最新状态后按幂等路径返回同一事实，
     * 避免两个事务同时改项目/阶段导致的死锁与"已提交却返回失败"。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stage from ProjectStage stage where stage.stageId = :stageId")
    Optional<ProjectStage> findLockedByStageId(@Param("stageId") Long stageId);

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
