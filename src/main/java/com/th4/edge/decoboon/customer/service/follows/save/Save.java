package com.th4.edge.decoboon.customer.service.follows.save;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新增客户跟进服务。 */
public interface Save extends ModelService<CustomerFollow, CustomerFollow>, Security {
}
