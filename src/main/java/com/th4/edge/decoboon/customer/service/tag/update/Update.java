package com.th4.edge.decoboon.customer.service.tag.update;

import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 更新客户标签字典项（仅企业负责人 / 门店店长）。 */
public interface Update extends Provider<CustomerTag, CustomerTag>, Security {
}
