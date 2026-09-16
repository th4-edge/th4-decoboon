package com.th4.edge.decoboon.project.service.tasks.save;

import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 施工任务编排服务。 */
public interface Save extends Provider<ProjectTask, ProjectTask>, Security {
}
