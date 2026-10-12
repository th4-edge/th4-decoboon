package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** 角色报表：一次取多项目验收（验收倒序），替代逐项目分页查询（DEC-036 正模式）。 */
    List<Acceptance> findByProject_ProjectIdInOrderByAcceptanceIdDesc(java.util.Collection<Long> projectIds);

    /** 角色报表：按状态聚合计数（DEC-036 正模式：计数用聚合 SQL，不拉实体行）。 */
    @Query("select a.status, count(a) from Acceptance a where a.project.projectId in :projectIds group by a.status")
    List<Object[]> countGroupByStatus(@Param("projectIds") java.util.Collection<Long> projectIds);

    /** 角色报表：按状态取全局 TopN（ORDER BY + LIMIT，行数有界）。 */
    @Query("select a from Acceptance a where a.project.projectId in :projectIds and a.status in :statuses "
            + "order by a.acceptanceId desc")
    List<Acceptance> findTopByProjectIdsAndStatusIn(@Param("projectIds") java.util.Collection<Long> projectIds,
                                                    @Param("statuses") java.util.Collection<
                                                            com.th4.edge.decoboon.project.configure.AcceptStatus> statuses,
                                                    Pageable pageable);

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


    /** 指定项目集合的验收单（门店维度下先由 SQL 限定项目范围，避免全企业加载）。 */
    @Query("select a from Acceptance a where a.company.comId = :comId and a.project.projectId in :projectIds")
    List<Acceptance> findByCompanyAndProjectIds(@Param("comId") Long comId,
                                                @Param("projectIds") java.util.Collection<Long> projectIds);
}
