package com.th4.edge.decoboon.finance.service.summary.report;

import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 财务工作台汇总。 */
public interface Report extends Provider<Project, FinanceSummary>, Security {
}
