package com.th4.edge.decoboon.payroll.service.compensations.delete.support;

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

/** 删除尚未形成考勤、提成或工资事实的薪酬方案。 */
@Note("薪酬方案删除")
@Properties(allow = {
        @AllowProperties(model = CompensationScheme.class,
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo",
                        "status", "remark", "locked", "lockedReason", "createdBy", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("payroll/compensation/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.payroll.service.compensations.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long schemeId;

    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;
    private final PayrollCompensationContext compensationContext;
    private final PayrollCompensationAuditContext auditContext;

    public Delete(CompensationSchemeRepository repository,
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
        auditContext.record(current, scheme, "DELETE", auditContext.snapshot(scheme));
        repository.delete(scheme);
        return scheme;
    }
}
