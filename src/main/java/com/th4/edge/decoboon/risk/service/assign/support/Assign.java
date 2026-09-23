package com.th4.edge.decoboon.risk.service.assign.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskAssignRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 风险指派实现。入参由 Provider 字段 @AutoInject 注入（工程约定）。 */
@Note("风险指派")
@Properties(allow = {
        @AllowProperties(model = RiskItem.class,
                properties = {"riskId", "alertKey", "alertType", "status", "owner", "store", "team",
                        "assignedAt", "dueAt", "roundNo", "recurrenceCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("risk/item/assign")
@Scope(ScopeTypes.PROTOTYPE)
public class Assign implements com.th4.edge.decoboon.risk.service.assign.Assign {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;

    @AutoInject
    private Long assigneeId;

    @AutoInject
    private String note;


    private final RiskDispositionContext context;

    public Assign(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public RiskItem call(RiskAssignRequest request) {
        return context.assign(employee, riskId, assigneeId, note);
    }
}
