package com.th4.edge.decoboon.payroll.service.commissions.report;

import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.FinanceCommissionReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 员工提成筛选汇总。 */
public interface Report extends Provider<CommissionItem, FinanceCommissionReport>, Security {
}
