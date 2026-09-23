package com.th4.edge.decoboon.risk.service.claim.support;

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

/** 风险认领实现。 */
@Note("风险认领")
@Properties(allow = {
        @AllowProperties(model = RiskItem.class,
                properties = {"riskId", "alertKey", "alertType", "status", "owner", "assignedAt", "dueAt", "roundNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("risk/item/claim")
@Scope(ScopeTypes.PROTOTYPE)
public class Claim implements com.th4.edge.decoboon.risk.service.claim.Claim {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;

    @AutoInject
    private String note;


    private final RiskDispositionContext context;

    public Claim(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public RiskItem call(RiskAssignRequest request) {
        return context.claim(employee, riskId, note);
    }
}
