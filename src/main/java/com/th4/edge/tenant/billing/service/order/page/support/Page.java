package com.th4.edge.tenant.billing.service.order.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.OrderItem;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.billing.repository.OrderItemRepository;
import com.th4.edge.tenant.billing.repository.OrderRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 企业订单分页 Provider。
 */
@Note("订单分页")
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
@Service("order/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.billing.service.order.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Page(TenantContext tenantContext,
                SubscriptionContext subscriptionContext,
                OrderRepository orderRepository,
                OrderItemRepository orderItemRepository) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Order> call(Order request) {
        Employee boss = tenantContext.requireBoss(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        org.springframework.data.domain.Page<Order> result = orderRepository
                .findByCompany_ComIdOrderByOrderIdDesc(boss.getCompany().getComId(),
                        PageRequest.of(page, pageSize));
        result.forEach(item -> {
            subscriptionContext.expireIfNecessary(item);
            item.setItems(orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(item.getOrderId()));
        });
        return result;
    }
}
