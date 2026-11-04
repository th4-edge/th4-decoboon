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
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final PayrollAttendanceAuditContext attendanceAuditContext;

    public PayrollBuildContext(PayrollAccessContext accessContext,
                               PayrollCalculationContext calculationContext,
                               EmployeeRepository employeeRepository,
                               CompensationSchemeRepository compensationRepository,
                               CommissionItemRepository commissionRepository,
                               CommissionAdjustmentRepository adjustmentRepository,
                               CommissionAdjustmentContext adjustmentContext,
                               AttendanceSummaryRepository attendanceRepository,
                               PayrollSheetRepository sheetRepository,
                               PayrollItemRepository itemRepository,
                               PayrollAttendanceAuditContext attendanceAuditContext) {
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
        this.attendanceAuditContext = attendanceAuditContext;
    }

    @Transactional
    public PayrollSheet build(Employee principal, String period) {
        Employee current = accessContext.requireEditor(principal);
        if (period == null || !period.matches("\\d{4}-\\d{2}")) {
            throw new DefinedException(new DefaultErrorCode("400", "工资周期格式必须为 YYYY-MM"));
        }
        YearMonth yearMonth = YearMonth.parse(period);
        LocalDate periodStartDate = yearMonth.atDay(1);
        LocalDate periodEndDate = yearMonth.atEndOfMonth();
        Long comId = current.getCompany().getComId();
        PayrollSheet sheet = sheetRepository.findByCompany_ComIdAndPeriod(comId, period).orElseGet(PayrollSheet::new);
        if (sheet.getSheetId() == null) {
            accessContext.applyCompanyOwnership(sheet, current);
            sheet.setPeriod(period);
            sheet = sheetRepository.save(sheet);
        } else if (sheet.getStatus() != PayrollSheetStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode(
                    "400", "当前工资单已提交复核或已形成工资事实，不能重新生成"));
        }

        List<PayrollBuildSource> sources = new ArrayList<>();
        int incompleteAttendance = 0;
        // 工资归属月份的在职判定以“薪酬方案生效期”为准（下方 findEffective），不以员工入职/离职时间判定。
        List<Employee> employees = employeeRepository.findByCompany_ComId(comId);
        for (Employee employee : employees) {
            CompensationScheme scheme = compensationRepository
                    .findEffective(comId, employee.getEmpId(), CompensationStatus.Active,
                            periodStartDate, periodEndDate)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (scheme == null) {
                continue;
            }
            AttendanceSummary attendance = attendanceRepository
                    .findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(comId, employee.getEmpId(), period)
                    .orElse(null);
            if (!attendanceReady(attendance)) {
                incompleteAttendance++;
                continue;
            }
            sources.add(new PayrollBuildSource(employee, scheme, attendance));
        }
        if (incompleteAttendance > 0) {
            throw new DefinedException(new DefaultErrorCode(
                    "400", "该工资月份有 " + incompleteAttendance
                    + " 名员工考勤未确认，请先完成月度考勤确认后重新生成工资单"));
        }
        List<PayrollItem> existingItems = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        Set<Long> sourceEmployeeIds = sources.stream()
                .map(source -> source.employee().getEmpId())
                .collect(Collectors.toSet());
        List<PayrollItem> staleItems = existingItems.stream()
                .filter(item -> item.getEmployee() != null
                        && !sourceEmployeeIds.contains(item.getEmployee().getEmpId()))
                .toList();
        if (!staleItems.isEmpty()) {
            Map<Long, AttendanceSummary> attendanceByEmployee = attendanceRepository
                    .findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(comId, period)
                    .stream()
                    .collect(Collectors.toMap(item -> item.getEmployee().getEmpId(), Function.identity(), (left, right) -> left));
            List<AttendanceSummary> restoredAttendances = new ArrayList<>();
            for (PayrollItem staleItem : staleItems) {
                if (staleItem.getStatus() != PayrollItemStatus.Draft) {
                    throw new DefinedException(new DefaultErrorCode("409", "工资单存在非草稿工资项，不能重新生成"));
                }
                adjustmentContext.resetForDraftItem(current, staleItem.getItemId());
                AttendanceSummary staleAttendance = attendanceByEmployee.get(staleItem.getEmployee().getEmpId());
                if (staleAttendance != null && staleAttendance.getStatus() == AttendanceStatus.InPayroll) {
                    String beforeSnapshot = attendanceAuditContext.snapshot(staleAttendance);
                    staleAttendance.setStatus(AttendanceStatus.Confirmed);
                    restoredAttendances.add(staleAttendance);
                    attendanceAuditContext.record(current, staleAttendance, "RESTORE_TO_CONFIRMED",
                            beforeSnapshot, "员工已退出当月薪酬方案，重算移除旧工资项");
                }
            }
            if (!restoredAttendances.isEmpty()) {
                attendanceRepository.saveAll(restoredAttendances);
            }
            itemRepository.deleteAll(staleItems);
            itemRepository.flush();
        }

        Map<Long, PayrollItem> existingItemsByEmployee = existingItems.stream()
                .filter(item -> item.getEmployee() != null
                        && sourceEmployeeIds.contains(item.getEmployee().getEmpId())
                        && item.getAttribution() == PayrollItemAttribution.Company)
                .collect(Collectors.toMap(item -> item.getEmployee().getEmpId(), Function.identity(), (left, right) -> left));

        boolean attendanceComplete = true;
        boolean commissionComplete = true;
        for (PayrollBuildSource source : sources) {
            Employee employee = source.employee();
            CompensationScheme scheme = source.scheme();
            AttendanceSummary attendance = source.attendance();
            // commissionComplete 仅作管理侧提示；DEC-047 / BR-039 禁止以此阻断工资确认。
            if (commissionRepository.existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(
                    comId, employee.getEmpId(),
                    List.of(CommissionStatus.Accrued, CommissionStatus.Released, CommissionStatus.InPayment))) {
                commissionComplete = false;
            }

            PayrollItem item = existingItemsByEmployee.get(employee.getEmpId());
            if (item == null) item = new PayrollItem();
            if (item.getItemId() != null && item.getStatus() != PayrollItemStatus.Draft) {
                throw new DefinedException(new DefaultErrorCode("409", "工资单存在非草稿工资项，不能重新生成"));
            }
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

        List<AttendanceSummary> lockedAttendances = new ArrayList<>();
        List<String> lockBeforeSnapshots = new ArrayList<>();
        for (PayrollBuildSource source : sources) {
            lockBeforeSnapshots.add(attendanceAuditContext.snapshot(source.attendance()));
            source.attendance().setStatus(AttendanceStatus.InPayroll);
            lockedAttendances.add(source.attendance());
        }
        if (!lockedAttendances.isEmpty()) {
            attendanceRepository.saveAll(lockedAttendances);
            for (int index = 0; index < lockedAttendances.size(); index++) {
                attendanceAuditContext.record(current, lockedAttendances.get(index),
                        "LOCK_IN_PAYROLL", lockBeforeSnapshots.get(index), null);
            }
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

    private boolean attendanceReady(AttendanceSummary attendance) {
        return attendance != null
                && (attendance.getStatus() == AttendanceStatus.Confirmed
                || attendance.getStatus() == AttendanceStatus.InPayroll);
    }

    private record PayrollBuildSource(Employee employee, CompensationScheme scheme,
                                      AttendanceSummary attendance) {
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
