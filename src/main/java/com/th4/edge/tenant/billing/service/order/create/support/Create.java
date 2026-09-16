package com.th4.edge.tenant.billing.service.order.create.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.OrderItem;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 企业负责人创建套餐、席位包和门店包订单。
 */
@Note("创建订单")
@Description("按套餐版本和加购商品生成待支付订单")
@Properties(allow = {
        @AllowProperties(model = Order.class,
                properties = {"orderId", "orderNo", "plan", "planVersion", "amount", "days",
                        "extraSeats", "extraStores", "status", "payChannel", "outTradeNo",
                        "transactionId", "paidTime", "expireTime", "items", "createTime"}),
        @AllowProperties(model = OrderItem.class,
                properties = {"orderItemId", "itemType", "planVersion", "addon", "productCode",
                        "productName", "unitPrice", "quantity", "amount", "days"}),
        @AllowProperties(model = Plan.class,
                properties = {"planId", "code", "name", "days", "maxStores", "maxSeats", "modules", "price"}),
        @AllowProperties(model = PlanVersion.class,
                properties = {"versionId", "versionNo", "planCode", "planName", "days", "maxStores",
                        "maxSeats", "modules", "price", "description"}),
        @AllowProperties(model = PlanAddon.class,
                properties = {"addonId", "code", "name", "type", "unitPrice", "days"})
})
@Service("order/create")
@Scope(ScopeTypes.PROTOTYPE)
public class Create implements com.th4.edge.tenant.billing.service.order.create.Create {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 32)
    private String planCode;

    @AutoInject
    private Integer seatQuantity;

    @AutoInject
    private Integer storeQuantity;

    @AutoInject
    private PayChannel payChannel;

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;

    public Create(TenantContext tenantContext, SubscriptionContext subscriptionContext) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public Order call(Order request) {
        Employee boss = tenantContext.requireBoss(employee);
        return subscriptionContext.createOrder(boss.getCompany(), planCode,
                seatQuantity, storeQuantity, payChannel);
    }
}
