package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 计划偏差仓储。 */
public interface ProjectPlanDeviationRepository
        extends JpaRepository<ProjectPlanDeviation, Long>, JpaSpecificationExecutor<ProjectPlanDeviation> {

    boolean existsByProject_ProjectIdAndStatus(Long projectId, PlanDeviationStatus status);

    List<ProjectPlanDeviation> findByProject_ProjectIdOrderByDeviationIdDesc(Long projectId);
}
