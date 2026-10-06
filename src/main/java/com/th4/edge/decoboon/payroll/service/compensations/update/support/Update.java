package com.th4.edge.decoboon.payroll.service.compensations.update.support;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 更新员工薪酬方案。 */
@Note("薪酬方案更新")
@Properties(allow = @AllowProperties(model = CompensationScheme.class,
        properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo", "status", "remark", "updateTime"}))
@Service("payroll/compensation/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.payroll.service.compensations.update.Update {

    @User private Employee employee;
    @AutoInject @NotNull private Long schemeId;
    @AutoInject private BigDecimal baseSalary;
    @AutoInject private Integer scheduledDays;
    @AutoInject private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject private CompensationStatus status;
    @AutoInject private String remark;

    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;

    public Update(CompensationSchemeRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public CompensationScheme call(CompensationScheme request) {
        Employee current = accessContext.requireEditor(employee);
        CompensationScheme scheme = repository.findById(schemeId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "薪酬方案不存在或无权访问")));
        if (baseSalary != null) scheme.setBaseSalary(baseSalary);
        if (scheduledDays != null) {
            if (scheduledDays <= 0 || scheduledDays > 31) {
                throw new DefinedException(new DefaultErrorCode("400", "月应出勤天数必须为 1 至 31 天"));
            }
            scheme.setScheduledDays(scheduledDays);
        }
        if (effectiveFrom != null) scheme.setEffectiveFrom(effectiveFrom);
        if (effectiveTo != null) scheme.setEffectiveTo(effectiveTo);
        if (status != null) scheme.setStatus(status);
        if (remark != null) scheme.setRemark(remark);
        return repository.save(scheme);
    }
}
