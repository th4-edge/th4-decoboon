package com.th4.edge.decoboon.customer.service.cust.delete;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人删除无业务事实的客户。 */
public interface Delete extends Provider<Customer, Customer>, Security {
}
