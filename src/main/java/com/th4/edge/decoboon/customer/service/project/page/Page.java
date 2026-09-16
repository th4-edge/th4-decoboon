package com.th4.edge.decoboon.customer.service.project.page;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主可见项目分页。 */
public interface Page extends Provider<Project, org.springframework.data.domain.Page<Project>>, Security {
}
