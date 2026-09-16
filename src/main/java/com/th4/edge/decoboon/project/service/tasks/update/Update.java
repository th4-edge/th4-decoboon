package com.th4.edge.decoboon.project.service.tasks.update;

import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/**
 * 施工任务推进服务。
 */
public interface Update extends Provider<ProjectTask, ProjectTask>, Security {
}
