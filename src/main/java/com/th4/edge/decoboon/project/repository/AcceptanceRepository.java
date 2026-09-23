package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 验收单仓储。
 */
public interface AcceptanceRepository extends JpaRepository<Acceptance, Long>,
        JpaSpecificationExecutor<Acceptance> {

    boolean existsByStageAndStatus(ProjectStage stage, com.th4.edge.decoboon.project.configure.AcceptStatus status);

    Optional<Acceptance> findFirstByStageOrderByAcceptanceIdDesc(ProjectStage stage);

    Page<Acceptance> findByProject_ProjectIdOrderByAcceptanceIdDesc(Long projectId, Pageable pageable);

    List<Acceptance> findByCompany_ComId(Long comId);

    List<Acceptance> findTop200ByCompany_ComIdOrderByAcceptanceIdDesc(Long comId);

    @EntityGraph(attributePaths = {"stage", "submittedBy"})
    List<Acceptance> findByProject_ProjectIdOrderByAcceptanceIdDesc(Long projectId);

    Optional<Acceptance> findByAcceptanceIdAndProject_ProjectId(Long acceptanceId, Long projectId);

    @EntityGraph(attributePaths = {
            "project", "property", "stage", "submittedBy",
            "project.manager", "project.designer", "project.support"
    })
    @Query("""
            select a from Acceptance a
            where a.acceptanceId = :acceptanceId
              and exists (
                select access.accessId from CustomerAccess access
                where access.project = a.project
                  and access.credential.creId = :creId
                  and access.status = :status
              )
            """)
    Optional<Acceptance> findForCustomerAccess(
            @Param("acceptanceId") Long acceptanceId,
            @Param("creId") Long creId,
            @Param("status") com.th4.edge.decoboon.customer.configure.CustomerAccessStatus status);

    @EntityGraph(attributePaths = {
            "project", "property", "stage", "submittedBy",
            "project.manager", "project.designer", "project.support"
    })
    @Query("select a from Acceptance a where a.stage = :stage order by a.acceptanceId desc")
    Optional<Acceptance> findFirstForCustomerByStage(@Param("stage") ProjectStage stage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from Acceptance a
            where a.acceptanceId = :acceptanceId
              and exists (
                select access.accessId from CustomerAccess access
                where access.project = a.project
                  and access.credential.creId = :creId
                  and access.status = :status
              )
            """)
    Optional<Acceptance> findLockedForCustomerAccess(
            @Param("acceptanceId") Long acceptanceId,
            @Param("creId") Long creId,
            @Param("status") com.th4.edge.decoboon.customer.configure.CustomerAccessStatus status);
}
