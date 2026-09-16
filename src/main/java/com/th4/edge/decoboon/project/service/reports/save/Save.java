package com.th4.edge.decoboon.project.service.reports.save;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 发布工地动态服务。 */
public interface Save extends ModelService<ProjectReport, ProjectReport>, Security {
}
