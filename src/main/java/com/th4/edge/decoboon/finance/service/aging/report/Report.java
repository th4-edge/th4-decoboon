package com.th4.edge.decoboon.finance.service.aging.report;

import com.th4.edge.decoboon.finance.model.FinanceAgingQuery;
import com.th4.edge.decoboon.finance.model.FinanceAgingReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 应收应付账龄报表。 */
public interface Report extends Provider<FinanceAgingQuery, FinanceAgingReport>, Security {
}
