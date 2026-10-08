package com.th4.edge.decoboon.customer.service.cust.statuslog.page;

import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 客户状态变更日志分页。 */
public interface Page extends Provider<CustomerStatusLog, org.springframework.data.domain.Page<CustomerStatusLog>>, Security {
}
