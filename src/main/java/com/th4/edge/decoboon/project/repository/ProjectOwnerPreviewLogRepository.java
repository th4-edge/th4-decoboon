package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ProjectOwnerPreviewLog;
import org.springframework.data.repository.Repository;

import java.util.List;

/** 业主视角预览审计只暴露追加与查询能力，不提供更新、删除入口。 */
public interface ProjectOwnerPreviewLogRepository extends Repository<ProjectOwnerPreviewLog, Long> {

    ProjectOwnerPreviewLog save(ProjectOwnerPreviewLog log);

    long count();

    List<ProjectOwnerPreviewLog> findByCompany_ComIdAndRequestedProjectIdOrderByLogIdAsc(
            Long comId, Long requestedProjectId);
}
