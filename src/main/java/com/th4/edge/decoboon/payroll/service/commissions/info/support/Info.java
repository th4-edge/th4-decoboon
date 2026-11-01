package com.th4.edge.decoboon.payroll.service.commissions.info.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Note("提成详情")
@Properties(allow = {
        @AllowProperties(model = CommissionItem.class,
                properties = {"commissionId", "employee", "project", "contractAmount", "manualRate", "commissionAmount",
                        "accruedAmount", "releasedAmount", "paidAmount", "status", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "status"})
})
@Service("payroll/commission/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.payroll.service.commissions.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long commissionId;
    private final CommissionItemRepository repository;
    private final PayrollAccessContext accessContext;
    public Info(CommissionItemRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override
    public CommissionItem call(CommissionItem request) {
        Employee current = accessContext.requireCommissionViewer(employee);
        Specification<CommissionItem> byId =
                (root, query, builder) -> builder.equal(root.get("commissionId"), commissionId);
        return repository.findOne(byId.and(accessContext.visible(current, CommissionItem.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "提成不存在或无权访问")));
    }
}
