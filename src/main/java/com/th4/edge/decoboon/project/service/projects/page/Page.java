package com.th4.edge.decoboon.project.service.projects.page;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/**
 * 项目分页服务。
 */
public interface Page extends ModelService<Project, org.springframework.data.domain.Page<Project>>, Security {
}
