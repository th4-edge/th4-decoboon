package com.th4.edge.decoboon.project.service.tasks.page;

import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/**
 * 施工任务分页服务。
 */
public interface Page extends ModelService<ProjectTask, org.springframework.data.domain.Page<ProjectTask>>, Security {
}
