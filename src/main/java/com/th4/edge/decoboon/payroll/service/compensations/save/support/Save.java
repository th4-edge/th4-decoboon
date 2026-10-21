package com.th4.edge.decoboon.payroll.service.compensations.save.support;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增员工薪酬方案。 */
@Note("薪酬方案新增")
@Properties(allow = {
        @AllowProperties(model = CompensationScheme.class,
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo",
                        "status", "remark", "locked", "lockedReason", "createdBy", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("payroll/compensation/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.payroll.service.compensations.save.Save {

    @User
    private Employee employee;
    @AutoInject @NotNull private Long empId;
    @AutoInject @NotNull private BigDecimal baseSalary;
    @AutoInject private Integer scheduledDays;
    @AutoInject @NotNull private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject private String remark;

    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;
    private final PayrollCompensationContext compensationContext;
    private final PayrollCompensationAuditContext auditContext;

    public Save(CompensationSchemeRepository repository,
                PayrollAccessContext accessContext,
                PayrollCompensationContext compensationContext,
                PayrollCompensationAuditContext auditContext) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.compensationContext = compensationContext;
        this.auditContext = auditContext;
    }

    @Override
    @Transactional
    public CompensationScheme call(@ModelSaveInject CompensationScheme request) {
        Employee current = accessContext.requireEditor(employee);
        Employee target = accessContext.resolveEmployee(current, empId);
        BigDecimal salary = compensationContext.validBaseSalary(baseSalary);
        int days = compensationContext.validScheduledDays(scheduledDays);
        compensationContext.validPeriod(effectiveFrom, effectiveTo);
        compensationContext.requireUniqueEffectiveDate(
                current.getCompany().getComId(), target, effectiveFrom, null);
        accessContext.applyOwnership(request, current, target);
        request.setBaseSalary(salary);
        request.setScheduledDays(days);
        request.setEffectiveFrom(effectiveFrom);
        request.setEffectiveTo(effectiveTo);
        request.setStatus(CompensationStatus.Active);
        request.setRemark(remark);
        request.setLocked(Boolean.FALSE);
        request.setLockedReason(null);
        CompensationScheme saved = repository.save(request);
        auditContext.record(current, saved, "CREATE", "-");
        return saved;
    }
}
