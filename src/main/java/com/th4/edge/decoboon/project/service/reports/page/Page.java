package com.th4.edge.decoboon.project.service.reports.page;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 工地动态分页服务。 */
public interface Page extends ModelService<ProjectReport, org.springframework.data.domain.Page<ProjectReport>>, Security {
}
