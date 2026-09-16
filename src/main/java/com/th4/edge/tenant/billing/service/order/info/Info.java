package com.th4.edge.tenant.billing.service.order.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Order;

/**
 * 企业订单详情与支付结果确认。
 */
public interface Info extends Provider<Order, Order>, Security {
}
