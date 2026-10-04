package com.th4.edge.decoboon.finance.service.payables.report;

import com.th4.edge.decoboon.finance.model.FinancePayableReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 应付筛选汇总。 */
public interface Report extends Provider<Payable, FinancePayableReport>, Security {
}
