package com.th4.edge.decoboon.customer.service.cust.visit;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 记录客户到店事件。 */
public interface Visit extends Provider<Customer, Customer>, Security {
}
