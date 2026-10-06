package com.th4.edge.decoboon.payroll.service.compensations.save.support;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增员工薪酬方案。 */
@Note("薪酬方案新增")
@Properties(allow = @AllowProperties(model = CompensationScheme.class,
        properties = {"schemeId", "employee", "baseSalary", "effectiveFrom", "effectiveTo", "status", "remark", "createTime"}))
@Service("payroll/compensation/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.payroll.service.compensations.save.Save {

    @User
    private Employee employee;
    @AutoInject @NotNull private Long empId;
    @AutoInject @NotNull private BigDecimal baseSalary;
    @AutoInject @NotNull private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject private String remark;

    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;

    public Save(CompensationSchemeRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public CompensationScheme call(@ModelSaveInject CompensationScheme request) {
        Employee current = accessContext.requireEditor(employee);
        Employee target = accessContext.resolveEmployee(current, empId);
        accessContext.applyOwnership(request, current, target);
        request.setBaseSalary(baseSalary);
        request.setEffectiveFrom(effectiveFrom);
        request.setEffectiveTo(effectiveTo);
        request.setStatus(CompensationStatus.Active);
        request.setRemark(remark);
        return repository.save(request);
    }
}
