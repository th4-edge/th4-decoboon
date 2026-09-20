package com.th4.edge.decoboon.customer.service.portrait.save;

import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 维护客户画像（十项营销画像字段，全部可空）。 */
public interface Save extends Provider<CustomerPortrait, CustomerPortrait>, Security {
}
