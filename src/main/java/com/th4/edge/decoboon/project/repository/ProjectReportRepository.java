package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;

import java.util.List;
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

    /** 角色报表：一次取多项目日报（时间倒序），替代逐项目分页查询（DEC-036 正模式）。 */
    List<ProjectReport> findByCompany_ComIdAndProject_ProjectIdInOrderByCreateTime_DateTimeDesc(
            Long comId, java.util.Collection<Long> projectIds);

    /** 角色报表：一次按项目分组统计日报数，替代逐项目 count（DEC-036 正模式：计数用聚合 SQL）。 */
    @org.springframework.data.jpa.repository.Query("select r.project.projectId, count(r) from ProjectReport r "
            + "where r.company.comId = :comId and r.project.projectId in :projectIds "
            + "group by r.project.projectId")
    List<Object[]> countGroupByProject(@org.springframework.data.repository.query.Param("comId") Long comId,
                                       @org.springframework.data.repository.query.Param("projectIds")
                                       java.util.Collection<Long> projectIds);

    long countByProject_ProjectId(Long projectId);
}
