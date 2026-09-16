package com.th4.edge.decoboon.finance.service.projects.report;

import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 单项目财务全景。 */
public interface Report extends Provider<Project, FinanceProjectReport>, Security {
}
