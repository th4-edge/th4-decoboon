package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 项目施工任务仓储。
 */
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>,
        JpaSpecificationExecutor<ProjectTask> {

    List<ProjectTask> findByStage(ProjectStage stage);

    List<ProjectTask> findByProject(Project project);
}
