package com.th4.edge.decoboon.customer.service.tag.page;

import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 客户标签字典分页。 */
public interface Page extends Provider<CustomerTag, org.springframework.data.domain.Page<CustomerTag>>, Security {
}
