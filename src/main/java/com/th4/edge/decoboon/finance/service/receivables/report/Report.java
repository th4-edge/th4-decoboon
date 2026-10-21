package com.th4.edge.decoboon.finance.service.receivables.report;

import com.th4.edge.decoboon.finance.model.FinanceReceivableReport;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 应收计划筛选汇总。 */
public interface Report extends Provider<Receivable, FinanceReceivableReport>, Security {
}
