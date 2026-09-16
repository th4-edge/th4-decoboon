package com.th4.edge.decoboon.project.service.reports.list;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 工地日报列表服务。 */
public interface List extends ModelService<ProjectReport, java.util.List<ProjectReport>>, Security {
}
