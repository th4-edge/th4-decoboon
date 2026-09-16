package com.th4.edge.decoboon.customer.service.project.info;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主可见项目详情。 */
public interface Info extends Provider<Project, Project>, Security {
}
