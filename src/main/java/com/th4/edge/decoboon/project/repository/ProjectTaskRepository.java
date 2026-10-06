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

    /** 角色报表：一次取多项目任务，替代逐项目查询（DEC-036 正模式）。 */
    List<ProjectTask> findByProject_ProjectIdIn(java.util.Collection<Long> projectIds);

    /** 同一项目 + 阶段 + 工艺条目下的全部任务，用于下发去重。 */
    List<ProjectTask> findByProject_ProjectIdAndStage_StageIdAndCatalogItem_ItemId(
            Long projectId, Long stageId, Long catalogItemId);
}
