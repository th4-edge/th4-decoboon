package com.th4.edge.decoboon.finance.service.receipts.report;

import com.th4.edge.decoboon.finance.model.FinanceReceiptReport;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 收款筛选汇总。 */
public interface Report extends Provider<Receipt, FinanceReceiptReport>, Security {
}
