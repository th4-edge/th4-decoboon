package com.th4.edge.decoboon.payroll.service.compensations.update.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 更新未形成薪酬事实的员工薪酬方案。 */
@Note("薪酬方案更新")
@Properties(allow = {
        @AllowProperties(model = CompensationScheme.class,
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo",
                        "status", "remark", "locked", "lockedReason", "createdBy", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("payroll/compensation/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.payroll.service.compensations.update.Update {

    @User private Employee employee;
    @AutoInject @NotNull private Long schemeId;
    @AutoInject private BigDecimal baseSalary;
    @AutoInject private Integer scheduledDays;
    @AutoInject private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject private String remark;

    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;
    private final PayrollCompensationContext compensationContext;
    private final PayrollCompensationAuditContext auditContext;

    public Update(CompensationSchemeRepository repository,
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
    public CompensationScheme call(CompensationScheme request) {
        Employee current = accessContext.requireEditor(employee);
        Specification<CompensationScheme> byId =
                (root, query, builder) -> builder.equal(root.get("schemeId"), schemeId);
        CompensationScheme scheme = repository.findOne(
                        byId.and(accessContext.visible(current, CompensationScheme.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "薪酬方案不存在或无权访问")));
        compensationContext.requireUnlocked(current.getCompany().getComId(), scheme.getEmployee());
        String before = auditContext.snapshot(scheme);

        BigDecimal nextSalary = baseSalary == null ? scheme.getBaseSalary()
                : compensationContext.validBaseSalary(baseSalary);
        int nextDays = scheduledDays == null ? scheme.getScheduledDays()
                : compensationContext.validScheduledDays(scheduledDays);
        LocalDate nextFrom = effectiveFrom == null ? scheme.getEffectiveFrom() : effectiveFrom;
        LocalDate nextTo = effectiveTo == null ? scheme.getEffectiveTo() : effectiveTo;
        compensationContext.validPeriod(nextFrom, nextTo);
        if (effectiveFrom != null && !nextFrom.equals(scheme.getEffectiveFrom())) {
            compensationContext.requireUniqueEffectiveDate(
                    current.getCompany().getComId(), scheme.getEmployee(), nextFrom, scheme.getSchemeId());
        }
        scheme.setBaseSalary(nextSalary);
        scheme.setScheduledDays(nextDays);
        scheme.setEffectiveFrom(nextFrom);
        scheme.setEffectiveTo(nextTo);
        if (remark != null) scheme.setRemark(remark);
        CompensationScheme saved = repository.save(scheme);
        auditContext.record(current, saved, "UPDATE", before);
        return saved;
    }
}
