package com.th4.edge.decoboon.customer.service.cust.wechat;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 记录客户微信添加事件。 */
public interface Wechat extends Provider<Customer, Customer>, Security {
}
