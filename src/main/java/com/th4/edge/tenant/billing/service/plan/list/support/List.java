package com.th4.edge.tenant.billing.service.plan.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 可续费套餐列表 Provider。
 */
@Note("套餐列表")
@Properties(allow = @AllowProperties(model = Plan.class,
        properties = {"planId", "code", "name", "days", "maxStores", "maxSeats", "modules",
                "price", "originalPrice", "description", "tagline", "highlights", "badge", "recommended"}))
@Service("plan/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.tenant.billing.service.plan.list.List {

    private final SubscriptionContext subscriptionContext;

    public List(SubscriptionContext subscriptionContext) {
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public java.util.List<Plan> call(@User Employee employee) {
        return subscriptionContext.availablePlans();
    }
}
