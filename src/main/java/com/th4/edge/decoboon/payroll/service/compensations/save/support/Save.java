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
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
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
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo", "status", "remark", "createTime"}),
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
    @AutoInject @NotNull private Integer scheduledDays;
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
        request.setScheduledDays(validScheduledDays(scheduledDays));
        request.setEffectiveFrom(effectiveFrom);
        request.setEffectiveTo(effectiveTo);
        request.setStatus(CompensationStatus.Active);
        request.setRemark(remark);
        boolean duplicate = repository.findByCompany_ComIdAndEmployeeOrderByEffectiveFromDesc(
                        current.getCompany().getComId(), target)
                .stream()
                .anyMatch(item -> effectiveFrom.equals(item.getEffectiveFrom()));
        if (duplicate) {
            throw new DefinedException(new DefaultErrorCode("409",
                    "该员工在此生效日期已有薪酬方案，请修改生效日期或编辑原方案"));
        }
        return repository.save(request);
    }

    private int validScheduledDays(Integer value) {
        if (value == null || value <= 0 || value > 31) {
            throw new DefinedException(
                    new DefaultErrorCode("400", "月应出勤天数必须为 1 至 31 天"));
        }
        return value;
    }
}
