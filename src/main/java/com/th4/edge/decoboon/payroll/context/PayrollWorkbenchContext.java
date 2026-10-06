package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.PayrollWorkbenchReport;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** 薪酬工作台聚合。 */
@Component
public class PayrollWorkbenchContext {

    private final PayrollAccessContext accessContext;
    private final AttendanceSummaryRepository attendanceRepository;
    private final PayrollSheetRepository sheetRepository;
    private final CommissionItemRepository commissionRepository;
    private final PayslipRepository payslipRepository;
    private final PayableRepository payableRepository;

    public PayrollWorkbenchContext(PayrollAccessContext accessContext,
                                   AttendanceSummaryRepository attendanceRepository,
                                   PayrollSheetRepository sheetRepository,
                                   CommissionItemRepository commissionRepository,
                                   PayslipRepository payslipRepository,
                                   PayableRepository payableRepository) {
        this.accessContext = accessContext;
        this.attendanceRepository = attendanceRepository;
        this.sheetRepository = sheetRepository;
        this.commissionRepository = commissionRepository;
        this.payslipRepository = payslipRepository;
        this.payableRepository = payableRepository;
    }

    @Transactional(readOnly = true)
    public PayrollWorkbenchReport report(Employee principal, String period) {
        Employee current = accessContext.requireManager(principal);
        String targetPeriod = period == null || period.isBlank() ? YearMonth.now().toString() : period;
        PayrollWorkbenchReport report = new PayrollWorkbenchReport();
        report.setPeriod(targetPeriod);

        report.setPendingAttendanceCount((int) attendanceRepository
                .findAll(accessContext.visible(current, AttendanceSummary.class)).stream()
                .filter(item -> targetPeriod.equals(item.getPayrollMonth()))
                .filter(item -> item.getStatus() == AttendanceStatus.Pending
                        || item.getStatus() == AttendanceStatus.Disputed)
                .count());

        List<PayrollSheet> visibleSheets = sheetRepository.findAll(accessContext.visible(current, PayrollSheet.class));
        var sheets = visibleSheets.stream()
                .filter(item -> targetPeriod.equals(item.getPeriod()))
                .toList();
        report.setCurrentPeriodHasSheet(!sheets.isEmpty());
        visibleSheets.stream()
                .filter(item -> item.getPeriod() != null && !item.getPeriod().isBlank())
                .max(Comparator.comparing(PayrollSheet::getPeriod))
                .ifPresent(latest -> {
                    report.setLatestSheetId(latest.getSheetId());
                    report.setLatestSheetPeriod(latest.getPeriod());
                    report.setLatestSheetStatus(latest.getStatus());
                    report.setLatestSheetTotalNetAmount(latest.getTotalNetAmount());
                });
        report.setDraftSheetCount((int) sheets.stream().filter(item -> item.getStatus() == PayrollSheetStatus.Draft).count());
        report.setPendingSheetCount((int) sheets.stream().filter(item -> item.getStatus() == PayrollSheetStatus.Pending).count());
        report.setConfirmedSheetCount((int) sheets.stream().filter(item -> item.getStatus() == PayrollSheetStatus.Confirmed).count());
        report.setPartialPaidSheetCount((int) sheets.stream().filter(item -> item.getStatus() == PayrollSheetStatus.PartialPaid).count());
        report.setPaidSheetCount((int) sheets.stream().filter(item -> item.getStatus() == PayrollSheetStatus.Paid).count());

        BigDecimal payableAmount = payableRepository.findAll(accessContext.visible(current, Payable.class)).stream()
                .filter(item -> item.getType() == PayableType.EmployeeCommission
                        || (item.getType() == PayableType.EmployeeSalary && targetPeriod.equals(item.getPeriod())))
                .map(item -> item.getAmount().subtract(item.getPaidAmount() == null ? BigDecimal.ZERO : item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setPendingEmployeePayableAmount(payableAmount);

        BigDecimal releasedUnpaid = commissionRepository
                .findAll(accessContext.visible(current, CommissionItem.class)).stream()
                .filter(item -> item.getStatus() == CommissionStatus.Released
                        || item.getStatus() == CommissionStatus.InPayment)
                .map(item -> item.getReleasedAmount().subtract(item.getPaidAmount() == null ? BigDecimal.ZERO : item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setReleasedUnpaidCommissionAmount(releasedUnpaid);

        var payslips = payslipRepository.findAll(accessContext.visible(current, Payslip.class)).stream()
                .filter(item -> targetPeriod.equals(item.getPeriod()))
                .toList();
        report.setPendingPayslipCount((int) payslips.stream().filter(item -> item.getStatus() == PayslipStatus.PendingPayment).count());
        report.setPublishedPayslipCount((int) payslips.stream().filter(item -> item.getStatus() == PayslipStatus.Published).count());
        return report;
    }
}
