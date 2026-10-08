package com.th4.edge.decoboon.customer.service.mycustomer.overview;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 设计师「我的客户」聚合 Provider（本人负责 ∪ 指派给我设计）。 */
public interface Overview extends Provider<Customer, CustomerOverviewReport>, Security {
}
