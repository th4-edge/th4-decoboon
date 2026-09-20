package com.th4.edge.decoboon.customer.service.tagging.delete;

import com.th4.edge.decoboon.customer.model.CustomerTagLink;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 移除客户人工标签。 */
public interface Delete extends Provider<CustomerTagLink, Boolean>, Security {
}
