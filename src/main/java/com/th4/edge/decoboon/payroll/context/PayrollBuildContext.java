package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemAttribution;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionAdjustmentRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 根据薪酬方案和已确认考勤生成工资单草稿。 */
@Component
public class PayrollBuildContext {

    private final PayrollAccessContext accessContext;
    private final PayrollCalculationContext calculationContext;
    private final EmployeeRepository employeeRepository;
    private final CompensationSchemeRepository compensationRepository;
    private final CommissionItemRepository commissionRepository;
    private final CommissionAdjustmentRepository adjustmentRepository;
    private final CommissionAdjustmentContext adjustmentContext;
    private final AttendanceSummaryRepository attendanceRepository;
    private final PayrollSheetRepository sheetRepository;
    private final PayrollItemRepository itemRepository;

    public PayrollBuildContext(PayrollAccessContext accessContext,
                               PayrollCalculationContext calculationContext,
                               EmployeeRepository employeeRepository,
                               CompensationSchemeRepository compensationRepository,
                               CommissionItemRepository commissionRepository,
                               CommissionAdjustmentRepository adjustmentRepository,
                               CommissionAdjustmentContext adjustmentContext,
                               AttendanceSummaryRepository attendanceRepository,
                               PayrollSheetRepository sheetRepository,
                               PayrollItemRepository itemRepository) {
        this.accessContext = accessContext;
        this.calculationContext = calculationContext;
        this.employeeRepository = employeeRepository;
        this.compensationRepository = compensationRepository;
        this.commissionRepository = commissionRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.adjustmentContext = adjustmentContext;
        this.attendanceRepository = attendanceRepository;
        this.sheetRepository = sheetRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public PayrollSheet build(Employee principal, String period) {
        Employee current = accessContext.requireEditor(principal);
        if (period == null || !period.matches("\\d{4}-\\d{2}")) {
            throw new DefinedException(new DefaultErrorCode("400", "工资周期格式必须为 YYYY-MM"));
        }
        Long comId = current.getCompany().getComId();
        PayrollSheet sheet = sheetRepository.findByCompany_ComIdAndPeriod(comId, period).orElseGet(PayrollSheet::new);
        if (sheet.getSheetId() == null) {
            accessContext.applyCompanyOwnership(sheet, current);
            sheet.setPeriod(period);
            sheet = sheetRepository.save(sheet);
        } else if (sheet.getStatus() == PayrollSheetStatus.Confirmed
                || sheet.getStatus() == PayrollSheetStatus.PartialPaid
                || sheet.getStatus() == PayrollSheetStatus.Paid
                || sheet.getStatus() == PayrollSheetStatus.Closed) {
            throw new DefinedException(new DefaultErrorCode("400", "已确认或已发放的工资单不能重新生成"));
        }

        boolean attendanceComplete = true;
        boolean commissionComplete = true;
        List<Employee> employees = employeeRepository.findByCompany_ComId(comId).stream()
                .filter(item -> item.getStatus() == EmployeeStatus.Active)
                .toList();
        for (Employee employee : employees) {
            CompensationScheme scheme = compensationRepository
                    .findByCompany_ComIdAndEmployee_EmpIdAndStatus(comId, employee.getEmpId(), CompensationStatus.Active)
                    .orElse(null);
            if (scheme == null) {
                continue;
            }
            AttendanceSummary attendance = attendanceRepository
                    .findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(comId, employee.getEmpId(), period)
                    .orElse(null);
            if (attendance == null || attendance.getStatus() != AttendanceStatus.Confirmed) {
                attendanceComplete = false;
            }
            if (commissionRepository.existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(
                    comId, employee.getEmpId(),
                    List.of(CommissionStatus.Accrued, CommissionStatus.Released, CommissionStatus.InPayment))) {
                commissionComplete = false;
            }

            PayrollItem item = itemRepository
                    .findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpIdAndAttributionKey(
                            comId, sheet.getSheetId(), employee.getEmpId(), "COMPANY")
                    .orElseGet(PayrollItem::new);
            boolean isNew = item.getItemId() == null;
            if (isNew) {
                accessContext.applyOwnership(item, current, employee, sheet, null);
                item.setAttribution(PayrollItemAttribution.Company);
                item.setAttributionKey("COMPANY");
                item.setStatus(PayrollItemStatus.Draft);
            } else {
                adjustmentContext.resetForDraftItem(current, item.getItemId());
            }
            item.setBaseSalaryAmount(money(scheme.getBaseSalary()));
            item.setCommissionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            int scheduledDays = attendance == null || attendance.getScheduledDays() == null
                    || attendance.getScheduledDays() <= 0
                    ? scheme.getScheduledDays()
                    : attendance.getScheduledDays();
            BigDecimal daily = calculationContext.dailySalary(scheme.getBaseSalary(), scheduledDays);
            item.setAttendanceDeduction(attendance == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : calculationContext.attendanceDeduction(daily, attendance, null, null, null));
            List<CommissionAdjustment> pendingAdjustments = adjustmentRepository
                    .findByCompany_ComIdAndEmployee_EmpIdAndStatusOrderByOccurredAtAscAdjustmentIdAsc(
                            comId, employee.getEmpId(), CommissionAdjustmentStatus.Pending);
            BigDecimal adjustmentDeduction = adjustmentContext.apply(pendingAdjustments, capacity(item));
            if (adjustmentDeduction.signum() > 0) {
                item.setCommissionAmount(adjustmentDeduction.negate().setScale(2, RoundingMode.HALF_UP));
            }
            calculationContext.calculate(item);
            item = itemRepository.save(item);
            List<CommissionAdjustment> appliedAdjustments = pendingAdjustments.stream()
                    .filter(adjustment -> money(adjustment.getAppliedAmount()).signum() > 0)
                    .toList();
            adjustmentContext.linkToPayrollItem(current, appliedAdjustments, item);
        }

        List<PayrollItem> items = itemRepository.findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        sheet.setAttendanceComplete(attendanceComplete);
        sheet.setCommissionComplete(commissionComplete);
        sheet.setTotalGrossAmount(sum(items, "gross"));
        sheet.setTotalDeductionAmount(sum(items, "deduction"));
        sheet.setTotalNetAmount(sum(items, "net"));
        sheet.setStatus(PayrollSheetStatus.Draft);
        return sheetRepository.save(sheet);
    }

    private BigDecimal sum(List<PayrollItem> items, String field) {
        BigDecimal result = BigDecimal.ZERO;
        for (PayrollItem item : items) {
            result = result.add(switch (field) {
                case "gross" -> money(item.getGrossAmount());
                case "deduction" -> money(item.getDeductionAmount());
                case "net" -> money(item.getNetAmount());
                default -> BigDecimal.ZERO;
            });
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal capacity(PayrollItem item) {
        return money(item.getBaseSalaryAmount())
                .add(money(item.getBonusAmount()))
                .add(money(item.getAllowanceAmount()))
                .subtract(money(item.getAttendanceDeduction()))
                .subtract(money(item.getSocialInsuranceAmount()))
                .subtract(money(item.getTaxAmount()))
                .subtract(money(item.getOtherDeduction()))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }
}
