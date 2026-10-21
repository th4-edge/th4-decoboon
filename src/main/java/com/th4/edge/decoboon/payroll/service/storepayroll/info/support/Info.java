package com.th4.edge.decoboon.payroll.service.storepayroll.info.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
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

@Note("本店薪酬工资详情")
@Properties(allow = {
        @AllowProperties(model = PayrollItem.class,
                properties = {"itemId", "employee", "sheet", "project", "attribution", "baseSalaryAmount", "attendanceDeduction",
                        "commissionAmount", "bonusAmount", "allowanceAmount", "socialInsuranceAmount", "taxAmount",
                        "otherDeduction", "grossAmount", "deductionAmount", "netAmount", "status", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.payroll.model.PayrollSheet.class,
                properties = {"sheetId", "period", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "name", "no", "status"})
})
@Service("payroll/store-payroll/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.payroll.service.storepayroll.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    private final PayrollItemRepository repository;
    private final PayrollAccessContext accessContext;
    public Info(PayrollItemRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override
    public PayrollItem call(PayrollItem request) {
        Employee current = accessContext.requireManager(employee);
        Specification<PayrollItem> byId =
                (root, query, builder) -> builder.equal(root.get("itemId"), itemId);
        return repository.findOne(byId.and(accessContext.visible(current, PayrollItem.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工资项不存在或无权访问")));
    }
}
