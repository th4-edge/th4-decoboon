package com.th4.edge.decoboon.project.service.overview;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectOverviewReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人项目经营聚合 Provider。 */
public interface Overview extends Provider<Project, ProjectOverviewReport>, Security {
}
