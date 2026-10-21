package com.th4.edge.decoboon.finance.service.refunds.report;

import com.th4.edge.decoboon.finance.model.FinanceRefundReport;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 客户退款筛选汇总。 */
public interface Report extends Provider<ReceiptRefund, FinanceRefundReport>, Security {
}
