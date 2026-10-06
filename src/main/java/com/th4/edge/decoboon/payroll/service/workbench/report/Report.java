package com.th4.edge.decoboon.payroll.service.workbench.report;

import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.PayrollWorkbenchReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Report extends Provider<PayrollSheet, PayrollWorkbenchReport>, Security {
}
