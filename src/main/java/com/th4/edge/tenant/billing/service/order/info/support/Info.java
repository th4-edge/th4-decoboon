package com.th4.edge.tenant.billing.service.order.info.support;

import com.th4.edge.components.wechat.service.WechatPayClient;
import com.th4.edge.components.wechat.service.WechatPayQuery;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.billing.configure.OrderStatus;
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

import java.util.Set;

/**
 * 企业订单详情与支付结果确认。
 */
@Note("订单详情")
@Description("查询本企业订单；微信订单支付后主动向微信支付确认并幂等履约")
@Properties(allow = {
        @AllowProperties(model = Order.class,
                properties = {"orderId", "orderNo", "plan", "planVersion", "amount", "days",
                        "extraSeats", "extraStores", "status", "payChannel", "outTradeNo",
                        "transactionId", "paidTime", "expireTime", "items", "createTime"}),
        @AllowProperties(model = OrderItem.class,
                properties = {"orderItemId", "itemType", "planVersion", "addon", "productCode",
                        "productName", "unitPrice", "quantity", "amount", "days"}),
        @AllowProperties(model = Plan.class,
                properties = {"planId", "code", "name", "days", "maxStores", "maxSeats", "modules",
                        "price", "originalPrice", "description", "tagline", "highlights", "badge", "recommended"}),
        @AllowProperties(model = PlanVersion.class,
                properties = {"versionId", "versionNo", "planCode", "planName", "days", "maxStores",
                        "maxSeats", "modules", "price", "description"}),
        @AllowProperties(model = PlanAddon.class,
                properties = {"addonId", "code", "name", "type", "unitPrice", "days"})
})
@Service("order/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.billing.service.order.info.Info {

    private static final Set<String> CLOSED_STATES = Set.of("CLOSED", "REVOKED", "PAYERROR");

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 40)
    private String orderNo;

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;
    private final WechatPayClient wechatPayClient;

    public Info(TenantContext tenantContext,
                SubscriptionContext subscriptionContext,
                WechatPayClient wechatPayClient) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
        this.wechatPayClient = wechatPayClient;
    }

    @Override
    public Order call(Order request) {
        Employee boss = tenantContext.requireBoss(employee);
        Order order = subscriptionContext.requireOrder(boss.getCompany(), orderNo);
        order = subscriptionContext.expireIfNecessary(order);
        if (order.getStatus() != OrderStatus.Pending || order.getPayChannel() != PayChannel.Wx) {
            return subscriptionContext.requireOrder(boss.getCompany(), orderNo);
        }
        WechatPayQuery payment;
        try {
            payment = wechatPayClient.query(order.getOutTradeNo());
        } catch (DefinedException exception) {
            return subscriptionContext.requireOrder(boss.getCompany(), orderNo);
        }
        if (payment.success()) {
            if (payment.amount() != order.getAmount()) {
                throw new DefinedException(new DefaultErrorCode("400", "微信支付金额与订单金额不一致"));
            }
            return subscriptionContext.fulfillOrder(order.getOrderNo(), payment.outTradeNo(),
                    payment.transactionId(), payment.amount(), PayChannel.Wx);
        }
        if (CLOSED_STATES.contains(payment.tradeState())) {
            return subscriptionContext.expire(order);
        }
        return order;
    }
}
