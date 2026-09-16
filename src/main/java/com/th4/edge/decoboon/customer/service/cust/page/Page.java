package com.th4.edge.decoboon.customer.service.cust.page;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/**
 * 客户分页服务。
 */
public interface Page extends Provider<Customer, org.springframework.data.domain.Page<Customer>>, Security {
}
