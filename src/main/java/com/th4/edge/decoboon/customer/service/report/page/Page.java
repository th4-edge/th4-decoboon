package com.th4.edge.decoboon.customer.service.report.page;

import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主可见项目动态分页。 */
public interface Page extends Provider<ProjectReport, org.springframework.data.domain.Page<ProjectReport>>, Security {
}
