package com.th4.edge.decoboon.payroll.service.payslips.info.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
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

@Note("工资条详情")
@Properties(allow = @AllowProperties(model = Payslip.class,
        properties = {"payslipId", "period", "grossAmount", "deductionAmount", "netAmount", "status", "publishedAt", "createTime"}))
@Service("payroll/payslip/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.payroll.service.payslips.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long payslipId;
    private final PayslipRepository repository;
    private final PayrollAccessContext accessContext;
    public Info(PayslipRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override
    public Payslip call(Payslip request) {
        Employee current = accessContext.requireSelf(employee);
        Payslip payslip = repository.findById(payslipId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工资条不存在或无权访问")));
        boolean manager = current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Boss
                || current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Finance;
        if (!manager && (payslip.getEmployee() == null
                || !payslip.getEmployee().getEmpId().equals(current.getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "工资条不存在或无权访问"));
        }
        if (!manager && payslip.getPublishedAt() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "工资条不存在或无权访问"));
        }
        return payslip;
    }
}
