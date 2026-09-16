package com.th4.edge.tenant.billing.service.addon.list.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 套餐加购商品列表 Provider。
 */
@Note("加购商品列表")
@Properties(allow = @AllowProperties(model = PlanAddon.class,
        properties = {"addonId", "code", "name", "type", "unitPrice", "days", "maxQuantity", "description"}))
@Service("addon/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.tenant.billing.service.addon.list.List {

    private final SubscriptionContext subscriptionContext;

    public List(SubscriptionContext subscriptionContext) {
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public java.util.List<PlanAddon> call(@User Employee employee) {
        return subscriptionContext.availableAddons();
    }
}
