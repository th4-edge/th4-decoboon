package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 项目材料计划仓储。
 */
public interface MaterialPlanRepository extends JpaRepository<MaterialPlan, Long>,
        JpaSpecificationExecutor<MaterialPlan> {

    List<MaterialPlan> findByProject(Project project);
}
