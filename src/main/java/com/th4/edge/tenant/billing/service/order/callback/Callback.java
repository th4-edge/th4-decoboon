package com.th4.edge.tenant.billing.service.order.callback;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.tenant.billing.model.Order;

/**
 * 支付回调履约。
 */
public interface Callback extends Provider<Order, Order> {
}
