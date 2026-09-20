package com.th4.edge.decoboon.customer.service.tag.list;

import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;


/** 可用标签字典列表（仅启用项，供业务员选用）。 */
public interface List extends Provider<CustomerTag, java.util.List<CustomerTag>>, Security {
}
