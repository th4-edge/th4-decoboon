package com.th4.edge.platform.service.plan.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 平台可售套餐列表实现。 */
@Note("平台可售套餐列表")
@Properties(allow = @AllowProperties(model = Plan.class, properties = {
        "planId", "code", "name", "days", "maxStores", "maxSeats", "modules", "price",
        "originalPrice", "description", "tagline", "highlights", "badge", "recommended", "priority"
}))
@Service("platform/plan/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.platform.service.plan.list.List {

    @User
    private Employee employee;

    private final PlatformAdminContext platformContext;
    private final SubscriptionContext subscriptionContext;

    public List(PlatformAdminContext platformContext, SubscriptionContext subscriptionContext) {
        this.platformContext = platformContext;
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public java.util.List<Plan> call(Plan request) {
        platformContext.requireAdmin(employee);
        return subscriptionContext.availablePlans();
    }
}
