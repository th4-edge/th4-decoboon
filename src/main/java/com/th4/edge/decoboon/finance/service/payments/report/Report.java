package com.th4.edge.decoboon.finance.service.payments.report;

import com.th4.edge.decoboon.finance.model.FinancePaymentReport;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 付款筛选汇总。 */
public interface Report extends Provider<Payment, FinancePaymentReport>, Security {
}
