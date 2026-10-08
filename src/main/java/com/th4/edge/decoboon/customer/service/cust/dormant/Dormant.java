package com.th4.edge.decoboon.customer.service.cust.dormant;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设置客户休眠状态。 */
public interface Dormant extends Provider<Customer, Customer>, Security {
}
