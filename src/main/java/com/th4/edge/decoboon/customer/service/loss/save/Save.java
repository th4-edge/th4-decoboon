package com.th4.edge.decoboon.customer.service.loss.save;

import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 标记客户签约前流失。 */
public interface Save extends Provider<CustomerLoss, CustomerLoss>, Security {
}
