package com.th4.edge.decoboon.customer.service.tag.save;

import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 新建客户标签字典项（仅企业负责人 / 店长）。 */
public interface Save extends Provider<CustomerTag, CustomerTag>, Security {
}
