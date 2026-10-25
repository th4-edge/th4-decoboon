package com.th4.edge.decoboon.customer.service.loss.page;

import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 客户流失历史分页。 */
public interface Page extends Provider<CustomerLoss, org.springframework.data.domain.Page<CustomerLoss>>, Security {
}
