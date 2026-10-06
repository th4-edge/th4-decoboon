package com.th4.edge.decoboon.payroll.service.compensations.delete.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 删除尚未形成考勤、提成或工资事实的薪酬方案。 */
@Note("薪酬方案删除")
@Properties(allow = @AllowProperties(model = CompensationScheme.class,
        properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo", "status"}))
@Service("payroll/compensation/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.payroll.service.compensations.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long schemeId;

    private final CompensationSchemeRepository repository;
    private final AttendanceSummaryRepository attendanceRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final CommissionItemRepository commissionRepository;
    private final PayrollAccessContext accessContext;

    public Delete(CompensationSchemeRepository repository,
                  AttendanceSummaryRepository attendanceRepository,
                  PayrollItemRepository payrollItemRepository,
                  CommissionItemRepository commissionRepository,
                  PayrollAccessContext accessContext) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.commissionRepository = commissionRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public CompensationScheme call(CompensationScheme request) {
        Employee current = accessContext.requireEditor(employee);
        Specification<CompensationScheme> byId = (root, query, builder) -> builder.equal(root.get("schemeId"), schemeId);
        CompensationScheme scheme = repository.findOne(byId.and(accessContext.visible(current, CompensationScheme.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "薪酬方案不存在或无权访问")));
        Long comId = current.getCompany().getComId();
        Long empId = scheme.getEmployee().getEmpId();
        if (attendanceRepository.existsByCompany_ComIdAndEmployee_EmpId(comId, empId)
                || payrollItemRepository.existsByCompany_ComIdAndEmployee_EmpId(comId, empId)
                || commissionRepository.existsByCompany_ComIdAndEmployee_EmpId(comId, empId)) {
            throw new DefinedException(new DefaultErrorCode("409", "已形成考勤、提成或工资事实，不能删除薪酬方案"));
        }
        repository.delete(scheme);
        return scheme;
    }
}
