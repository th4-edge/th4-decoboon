package com.th4.edge.decoboon.customer.service.loss.update;

import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 恢复客户流失记录。 */
public interface Update extends Provider<CustomerLoss, CustomerLoss>, Security {
}
