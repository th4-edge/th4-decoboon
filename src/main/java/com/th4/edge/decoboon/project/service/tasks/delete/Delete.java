package com.th4.edge.decoboon.project.service.tasks.delete;

import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 施工任务删除服务。 */
public interface Delete extends Provider<ProjectTask, ProjectTask>, Security {
}
