package com.th4.edge.decoboon.customer.service.invite.cancel.support;

import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主邀请撤销实现。 */
@Note("撤销业主邀请")
@Properties(allow = {
        @AllowProperties(model = CustomerInvite.class,
                properties = {"inviteId", "project", "customer", "customerName", "telNo", "status",
                        "expireTime", "acceptedAt", "cancelledAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("identity/customer-invite/cancel")
@Scope(ScopeTypes.PROTOTYPE)
public class Cancel implements com.th4.edge.decoboon.customer.service.invite.cancel.Cancel {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long inviteId;

    private final CustomerInviteContext inviteContext;

    public Cancel(CustomerInviteContext inviteContext) {
        this.inviteContext = inviteContext;
    }

    @Override
    @Transactional
    public CustomerInvite call(CustomerInvite request) {
        return inviteContext.cancel(employee, inviteId);
    }
}
