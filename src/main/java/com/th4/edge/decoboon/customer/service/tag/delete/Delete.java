package com.th4.edge.decoboon.customer.service.tag.delete;

import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 停用客户标签字典项（软删，仅企业负责人 / 门店店长）。 */
public interface Delete extends Provider<CustomerTag, CustomerTag>, Security {
}
