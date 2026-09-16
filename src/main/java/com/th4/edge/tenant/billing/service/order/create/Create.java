package com.th4.edge.tenant.billing.service.order.create;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Order;

/**
 * 创建套餐订单。
 */
public interface Create extends Provider<Order, Order>, Security {
}
