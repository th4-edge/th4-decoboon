package com.th4.edge.decoboon.project.service.manager.customers.page;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目经理负责项目关联客户分页。 */
public interface Page extends Provider<Customer, org.springframework.data.domain.Page<Customer>>, Security {
}
