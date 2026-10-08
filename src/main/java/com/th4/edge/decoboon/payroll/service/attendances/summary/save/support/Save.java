package com.th4.edge.decoboon.payroll.service.attendances.summary.save.support;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

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
    @AutoInject private Integer normalDays;
    @AutoInject private Integer personalLeaveDays;
    @AutoInject private Integer sickLeaveDays;
    @AutoInject private Integer absenceDays;
    @AutoInject private Integer externalDays;
    @AutoInject private Integer annualLeaveDays;
    @AutoInject private Integer compLeaveDays;
    @AutoInject private String remark;
    private final AttendanceSummaryRepository repository;
    private final CompensationSchemeRepository compensationRepository;
    private final PayrollAccessContext accessContext;
    public Save(AttendanceSummaryRepository repository,
                CompensationSchemeRepository compensationRepository,
                PayrollAccessContext accessContext) {
        this.repository = repository;
        this.compensationRepository = compensationRepository;
        this.accessContext = accessContext;
    }
    @Override @Transactional
    public AttendanceSummary call(@ModelSaveInject AttendanceSummary request) {
        Employee current = accessContext.requireEditor(employee);
        Employee target = accessContext.resolveEmployee(current, empId);
        if (repository.findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(
                current.getCompany().getComId(), empId, month).isPresent()) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "该员工本月考勤已存在"));
        }
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        CompensationScheme scheme = compensationRepository
                .findEffective(current.getCompany().getComId(), empId, CompensationStatus.Active,
                        periodStart, periodEnd)
                .stream()
                .findFirst()
                .orElseThrow(() -> new com.th4.edge.framework.exception.DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode(
                                "400", "该工资月份没有生效薪酬方案，请先配置对应生效期的方案")));
        if (scheme.getScheduledDays() == null || scheme.getScheduledDays() <= 0) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "薪酬方案未设置月应出勤天数"));
        }
        accessContext.applyOwnership(request, current, target);
        request.setPayrollMonth(month);
        request.setScheduledDays(scheme.getScheduledDays());
        request.setNormalDays(value(normalDays));
        request.setPersonalLeaveDays(value(personalLeaveDays));
        request.setSickLeaveDays(value(sickLeaveDays));
        request.setAbsenceDays(value(absenceDays));
        request.setExternalDays(value(externalDays));
        request.setAnnualLeaveDays(value(annualLeaveDays));
        request.setCompLeaveDays(value(compLeaveDays));
        request.setStatus(AttendanceStatus.Pending);
        request.setRemark(remark);
        return repository.save(request);
    }
    private int value(Integer value) { return value == null ? 0 : value; }
}
