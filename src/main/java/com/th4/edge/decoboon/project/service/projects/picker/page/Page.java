package com.th4.edge.decoboon.project.service.projects.picker.page;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目选择器轻量分页。 */
public interface Page extends Provider<Project, org.springframework.data.domain.Page<Project>>, Security {
}
