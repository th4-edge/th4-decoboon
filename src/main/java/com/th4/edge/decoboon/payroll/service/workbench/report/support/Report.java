package com.th4.edge.decoboon.payroll.service.workbench.report.support;

import com.th4.edge.decoboon.payroll.context.PayrollWorkbenchContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.PayrollWorkbenchReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Note("薪酬工作台")
@Properties(allow = @AllowProperties(model = PayrollWorkbenchReport.class,
        properties = {"period", "pendingAttendanceCount", "draftSheetCount", "pendingSheetCount", "confirmedSheetCount",
                "partialPaidSheetCount", "paidSheetCount", "pendingEmployeePayableAmount",
                "releasedUnpaidCommissionAmount", "pendingPayslipCount", "publishedPayslipCount",
                "currentPeriodHasSheet", "latestSheetId", "latestSheetPeriod", "latestSheetStatus",
                "latestSheetTotalNetAmount"}))
@Service("payroll/workbench/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.payroll.service.workbench.report.Report {
    @User private Employee employee;
    @AutoInject private String period;
    private final PayrollWorkbenchContext context;
    public Report(PayrollWorkbenchContext context) { this.context = context; }
    @Override
    public PayrollWorkbenchReport call(PayrollSheet request) {
        return context.report(employee, period);
    }
}
