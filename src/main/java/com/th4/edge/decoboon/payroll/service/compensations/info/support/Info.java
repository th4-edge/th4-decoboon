package com.th4.edge.decoboon.payroll.service.compensations.info.support;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
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
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Note("薪酬方案详情")
@Properties(allow = {
        @AllowProperties(model = CompensationScheme.class,
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo",
                        "status", "remark", "locked", "lockedReason", "createdBy", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("payroll/compensation/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.payroll.service.compensations.info.Info {
    @User private Employee employee;
    @AutoInject private Long schemeId;
    @AutoInject private Long empId;
    @AutoInject private String month;
    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;
    private final PayrollCompensationContext compensationContext;

    public Info(CompensationSchemeRepository repository,
                PayrollAccessContext accessContext,
                PayrollCompensationContext compensationContext) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.compensationContext = compensationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CompensationScheme call(CompensationScheme request) {
        Employee current = accessContext.requireManager(employee);
        CompensationScheme scheme;
        if (schemeId != null) {
            Specification<CompensationScheme> byId =
                    (root, query, builder) -> builder.equal(root.get("schemeId"), schemeId);
            scheme = repository.findOne(byId.and(accessContext.visible(current, CompensationScheme.class)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "薪酬方案不存在或无权访问")));
        } else {
            if (empId == null || month == null || !month.matches("\\d{4}-\\d{2}")) {
                throw new DefinedException(new DefaultErrorCode("400", "请提供薪酬方案索引，或员工和工资月份"));
            }
            Employee target = accessContext.resolveEmployee(current, empId);
            YearMonth yearMonth = YearMonth.parse(month);
            scheme = repository.findEffective(current.getCompany().getComId(), target.getEmpId(),
                            CompensationStatus.Active,
                            yearMonth.atDay(1), yearMonth.atEndOfMonth())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode(
                            "404", "该工资月份没有生效薪酬方案")));
        }
        compensationContext.enrichLockState(scheme, current.getCompany().getComId());
        return scheme;
    }
}
