package com.th4.edge.decoboon.finance.service.ledgers.report;

import com.th4.edge.decoboon.finance.model.FinanceLedgerReport;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 资金台账筛选汇总。 */
public interface Report extends Provider<Ledger, FinanceLedgerReport>, Security {
}
