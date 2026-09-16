package com.th4.edge.tenant.billing.service.order.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Order;

/**
 * 企业订单分页。
 */
public interface Page extends Provider<Order, org.springframework.data.domain.Page<Order>>, Security {
}
