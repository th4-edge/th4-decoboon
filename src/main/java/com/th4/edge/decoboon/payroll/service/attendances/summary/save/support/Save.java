package com.th4.edge.decoboon.payroll.service.attendances.summary.save.support;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceValidationContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Note("月度考勤新增")
@Properties(allow = {
        @AllowProperties(model = AttendanceSummary.class,
                properties = {"summaryId", "employee", "month", "scheduledDays", "normalDays", "personalLeaveDays", "sickLeaveDays",
                        "absenceDays", "externalDays", "annualLeaveDays", "compLeaveDays", "status", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("attendance/summary/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.payroll.service.attendances.summary.save.Save {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    @AutoInject @NotEmpty private String month;
    @AutoInject private BigDecimal scheduledDays;
    @AutoInject private BigDecimal normalDays;
    @AutoInject private BigDecimal personalLeaveDays;
    @AutoInject private BigDecimal sickLeaveDays;
    @AutoInject private BigDecimal absenceDays;
    @AutoInject private BigDecimal externalDays;
    @AutoInject private BigDecimal annualLeaveDays;
    @AutoInject private BigDecimal compLeaveDays;
    @AutoInject @Length(max = 500) private String remark;
    private final AttendanceSummaryRepository repository;
    private final CompensationSchemeRepository compensationRepository;
    private final PayrollAccessContext accessContext;
    private final PayrollAttendanceValidationContext validationContext;
    private final PayrollAttendanceAuditContext auditContext;
    public Save(AttendanceSummaryRepository repository,
                CompensationSchemeRepository compensationRepository,
                PayrollAccessContext accessContext,
                PayrollAttendanceValidationContext validationContext,
                PayrollAttendanceAuditContext auditContext) {
        this.repository = repository;
        this.compensationRepository = compensationRepository;
        this.accessContext = accessContext;
        this.validationContext = validationContext;
        this.auditContext = auditContext;
    }
    @Override @Transactional
    public AttendanceSummary call(@ModelSaveInject AttendanceSummary request) {
        Employee current = accessContext.requireManager(employee);
        Employee target = accessContext.resolveEmployee(current, empId);
        String normalizedMonth = normalizeMonth(month);
        if (repository.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                current.getCompany().getComId(), empId, normalizedMonth).isPresent()) {
            throw new DefinedException(new DefaultErrorCode("409", "该员工本月考勤已存在"));
        }
        YearMonth yearMonth = parseMonth(normalizedMonth);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        CompensationScheme scheme = compensationRepository
                .findEffective(current.getCompany().getComId(), empId, CompensationStatus.Active,
                        periodStart, periodEnd)
                .stream()
                .findFirst()
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode(
                        "400", "该工资月份没有生效薪酬方案，请先配置对应生效期的方案")));
        Integer effectiveScheduledDays = scheduledDays != null
                ? validationContext.integerScheduledDays(scheduledDays)
                : scheme.getScheduledDays();
        accessContext.applyOwnership(request, current, target);
        request.setPayrollMonth(normalizedMonth);
        request.setScheduledDays(effectiveScheduledDays == null ? 0 : effectiveScheduledDays);
        request.setNormalDays(value(normalDays));
        request.setPersonalLeaveDays(value(personalLeaveDays));
        request.setSickLeaveDays(value(sickLeaveDays));
        request.setAbsenceDays(value(absenceDays));
        request.setExternalDays(value(externalDays));
        request.setAnnualLeaveDays(value(annualLeaveDays));
        request.setCompLeaveDays(value(compLeaveDays));
        request.setStatus(AttendanceStatus.Pending);
        request.setRemark(remark);
        validationContext.validate(request);
        try {
            AttendanceSummary saved = repository.saveAndFlush(request);
            auditContext.record(current, saved, "CREATE", "-", null);
            return saved;
        } catch (DataIntegrityViolationException exception) {
            throw new DefinedException(new DefaultErrorCode("409", "该员工本月考勤已存在，请刷新后重试"));
        }
    }

    private String normalizeMonth(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new DefinedException(new DefaultErrorCode("400", "工资月份格式必须为 YYYY-MM"));
        }
        return normalized;
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DefinedException(new DefaultErrorCode("400", "工资月份格式必须为 YYYY-MM"));
        }
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
