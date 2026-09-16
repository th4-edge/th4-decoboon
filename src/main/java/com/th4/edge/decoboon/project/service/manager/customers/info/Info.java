package com.th4.edge.decoboon.project.service.manager.customers.info;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目经理负责项目关联客户详情。 */
public interface Info extends Provider<Customer, Customer>, Security {
}
