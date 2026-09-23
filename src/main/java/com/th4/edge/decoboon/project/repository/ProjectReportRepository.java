package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 工地动态仓储。 */
public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long>,
        JpaSpecificationExecutor<ProjectReport> {

    Page<ProjectReport> findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
            Long comId, Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"stage", "author"})
    Page<ProjectReport> findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
            Long comId, Long projectId, Pageable pageable);

    Page<ProjectReport> findByCompany_ComIdOrderByCreateTime_DateTimeDesc(Long comId, Pageable pageable);

    long countByProject_ProjectId(Long projectId);
}
