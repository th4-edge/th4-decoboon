package com.th4.edge.decoboon.risk.service.reopen.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
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

/** 重开风险实现。 */
@Note("重开风险")
@Properties(allow = {
        @AllowProperties(model = RiskItem.class,
                properties = {"riskId", "alertKey", "status", "owner", "dueAt", "roundNo", "reopenReason"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("risk/item/reopen")
@Scope(ScopeTypes.PROTOTYPE)
public class Reopen implements com.th4.edge.decoboon.risk.service.reopen.Reopen {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;

    @AutoInject
    private String reason;


    private final RiskDispositionContext context;

    public Reopen(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public RiskItem call(RiskDispositionRequest request) {
        return context.reopen(employee, riskId, reason);
    }
}
