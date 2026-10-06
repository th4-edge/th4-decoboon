package com.th4.edge.decoboon.payroll.service.payslips.page.support;

import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Note("我的已发放工资条")
@Properties(allow = @AllowProperties(model = Payslip.class,
        properties = {"payslipId", "period", "grossAmount", "deductionAmount", "netAmount", "status", "publishedAt", "createTime"}))
@Service("payroll/payslip/my/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.payslips.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final PayslipRepository repository;
    private final PayrollAccessContext accessContext;
    public Page(PayslipRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override
    public org.springframework.data.domain.Page<Payslip> call(Payslip request) {
        Employee currentEmployee = accessContext.requireSelf(employee);
        var items = repository
                .findByCompany_ComIdAndEmployee_EmpIdAndStatusAndPublishedAtIsNotNullOrderByPeriodDesc(
                        currentEmployee.getCompany().getComId(), currentEmployee.getEmpId(), PayslipStatus.Published);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
