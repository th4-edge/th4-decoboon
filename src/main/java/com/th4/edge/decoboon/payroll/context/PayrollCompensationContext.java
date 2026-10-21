package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 薪酬方案版本、事实锁定和输入校验上下文。 */
@Component
public class PayrollCompensationContext {

    private static final String LOCKED_REASON = "已形成考勤、提成或工资事实，方案已锁定；请新增更晚生效的后续方案";

    private final CompensationSchemeRepository schemeRepository;
    private final AttendanceSummaryRepository attendanceRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final CommissionItemRepository commissionRepository;

    public PayrollCompensationContext(CompensationSchemeRepository schemeRepository,
                                      AttendanceSummaryRepository attendanceRepository,
                                      PayrollItemRepository payrollItemRepository,
                                      CommissionItemRepository commissionRepository) {
        this.schemeRepository = schemeRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.commissionRepository = commissionRepository;
    }

    /** 批量补齐锁定状态，锁定判定固定为 3 条聚合查询，禁止逐行查库。 */
    public void enrichLockState(List<CompensationScheme> schemes, Long comId) {
        if (schemes == null || schemes.isEmpty()) return;
        Set<Long> employeeIds = schemes.stream()
                .map(CompensationScheme::getEmployee)
                .filter(Objects::nonNull)
                .map(Employee::getEmpId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<Long> locked = lockedEmployeeIds(comId, employeeIds);
        schemes.forEach(scheme -> {
            Employee employee = scheme.getEmployee();
            boolean isLocked = employee != null && employee.getEmpId() != null
                    && locked.contains(employee.getEmpId());
            scheme.setLocked(isLocked);
            scheme.setLockedReason(isLocked ? LOCKED_REASON : null);
        });
    }

    public void enrichLockState(CompensationScheme scheme, Long comId) {
        if (scheme == null) return;
        enrichLockState(List.of(scheme), comId);
    }

    /** 已有薪酬事实时禁止直接修改、终止或删除方案。 */
    public void requireUnlocked(Long comId, Employee employee) {
        if (employee == null || employee.getEmpId() == null
                || lockedEmployeeIds(comId, Set.of(employee.getEmpId())).contains(employee.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("409", LOCKED_REASON));
        }
    }

    /** 同员工同生效日期只能存在一个版本。 */
    public void requireUniqueEffectiveDate(Long comId, Employee employee,
                                           LocalDate effectiveFrom, Long excludeSchemeId) {
        boolean duplicate = schemeRepository
                .findByCompany_ComIdAndEmployeeOrderByEffectiveFromDesc(comId, employee)
                .stream()
                .filter(item -> excludeSchemeId == null || !excludeSchemeId.equals(item.getSchemeId()))
                .anyMatch(item -> effectiveFrom.equals(item.getEffectiveFrom()));
        if (duplicate) {
            throw new DefinedException(new DefaultErrorCode("409",
                    "该员工在此生效日期已有薪酬方案，请修改生效日期或编辑原方案"));
        }
    }

    public BigDecimal validBaseSalary(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "月固定工资必须大于 0"));
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public int validScheduledDays(Integer value) {
        if (value == null || value <= 0 || value > 31) {
            throw new DefinedException(new DefaultErrorCode("400", "月应出勤天数必须为 1 至 31 天"));
        }
        return value;
    }

    public void validPeriod(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveFrom == null) {
            throw new DefinedException(new DefaultErrorCode("400", "生效日期不能为空"));
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new DefinedException(new DefaultErrorCode("400", "失效日期不能早于生效日期"));
        }
    }

    private Set<Long> lockedEmployeeIds(Long comId, Set<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return Set.of();
        Set<Long> locked = new HashSet<>();
        locked.addAll(attendanceRepository.findEmployeeIdsWithFacts(comId, employeeIds));
        locked.addAll(payrollItemRepository.findEmployeeIdsWithFacts(comId, employeeIds));
        locked.addAll(commissionRepository.findEmployeeIdsWithFacts(comId, employeeIds));
        return locked;
    }
}
