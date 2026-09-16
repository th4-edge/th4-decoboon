package com.th4.edge.decoboon.customer.service.follows.page;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 客户跟进分页服务。 */
public interface Page extends ModelService<CustomerFollow, org.springframework.data.domain.Page<CustomerFollow>>, Security {
}
