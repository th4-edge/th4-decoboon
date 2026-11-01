package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.model.PayrollWorkbenchReport;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/** 薪酬工作台聚合。 */
@Component
public class PayrollWorkbenchContext {

    private final PayrollAccessContext accessContext;
    private final PayrollWorkbenchAggregateContext aggregateContext;
    private final StoreFilterContext storeFilterContext;

    public PayrollWorkbenchContext(PayrollAccessContext accessContext,
                                   PayrollWorkbenchAggregateContext aggregateContext,
                                   StoreFilterContext storeFilterContext) {
        this.accessContext = accessContext;
        this.aggregateContext = aggregateContext;
        this.storeFilterContext = storeFilterContext;
    }

    @Transactional(readOnly = true)
    public PayrollWorkbenchReport report(Employee principal, String period) {
        return report(principal, period, null);
    }

    @Transactional(readOnly = true)
    public PayrollWorkbenchReport report(Employee principal, String period, Long requestedStoreId) {
        Employee current = accessContext.requireManager(principal);
        Long storeFilter = storeFilterContext.resolve(current, requestedStoreId);
        String targetPeriod = period == null || period.isBlank() ? YearMonth.now().toString() : period;
        PayrollWorkbenchReport report = new PayrollWorkbenchReport();
        report.setPeriod(targetPeriod);

        PayrollWorkbenchAggregateContext.AttendanceAggregate attendance =
                aggregateContext.attendance(current, targetPeriod, storeFilter);
        PayrollWorkbenchAggregateContext.SheetAggregate sheets =
                aggregateContext.sheets(current, targetPeriod, storeFilter);
        PayrollWorkbenchAggregateContext.PayableAggregate payables =
                aggregateContext.payables(current, targetPeriod, storeFilter);
        PayrollWorkbenchAggregateContext.CommissionAggregate commissions =
                aggregateContext.commissions(current, storeFilter);
        PayrollWorkbenchAggregateContext.PayslipAggregate payslips =
                aggregateContext.payslips(current, targetPeriod, storeFilter);
        PayrollWorkbenchAggregateContext.LatestSheet latest =
                aggregateContext.latestSheet(current, storeFilter);

        report.setPendingAttendanceCount(attendance.pendingCount());
        report.setCurrentPeriodHasSheet(sheets.currentPeriodHasSheet());
        report.setDraftSheetCount(sheets.draftCount());
        report.setPendingSheetCount(sheets.pendingCount());
        report.setConfirmedSheetCount(sheets.confirmedCount());
        report.setPartialPaidSheetCount(sheets.partialPaidCount());
        report.setPaidSheetCount(sheets.paidCount());
        report.setPendingEmployeePayableAmount(payables.pendingEmployeePayableAmount());
        report.setReleasedUnpaidCommissionAmount(commissions.releasedUnpaidCommissionAmount());
        report.setPendingPayslipCount(payslips.pendingCount());
        report.setPublishedPayslipCount(payslips.publishedCount());
        report.setLatestSheetId(latest.sheetId());
        report.setLatestSheetPeriod(latest.period());
        report.setLatestSheetStatus(latest.status());
        report.setLatestSheetTotalNetAmount(latest.totalNetAmount());
        return report;
    }
}
