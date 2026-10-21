package com.th4.edge.decoboon.finance.service.projectledger.report;

import com.th4.edge.decoboon.finance.model.FinanceProjectLedgerReport;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目账本筛选汇总。 */
public interface Report extends Provider<Project, FinanceProjectLedgerReport>, Security {
}
