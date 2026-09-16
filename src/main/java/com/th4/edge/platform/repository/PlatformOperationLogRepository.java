package com.th4.edge.platform.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.platform.model.PlatformOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 平台独立操作审计仓储。 */
public interface PlatformOperationLogRepository extends JpaRepository<PlatformOperationLog, Long> {

    @EntityGraph(attributePaths = {"org", "operator", "operator.department"})
    @Query("""
            select log from PlatformOperationLog log
            where log.org.orgId = :orgId
              and (:companyId is null or log.targetComId = :companyId)
              and (:action is null or log.action = :action)
            order by log.logId desc
            """)
    Page<PlatformOperationLog> pageLogs(@Param("orgId") Long orgId,
                                        @Param("companyId") Long companyId,
                                        @Param("action") PlatformAction action,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"org", "operator", "operator.department"})
    List<PlatformOperationLog> findTop20ByOrg_OrgIdOrderByLogIdDesc(Long orgId);
}
