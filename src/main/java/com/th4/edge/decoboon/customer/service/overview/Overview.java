package com.th4.edge.decoboon.customer.service.overview;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业负责人客户经营聚合 Provider。 */
public interface Overview extends Provider<Customer, CustomerOverviewReport>, Security {
}
