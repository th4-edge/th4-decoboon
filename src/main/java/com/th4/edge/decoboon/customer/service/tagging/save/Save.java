package com.th4.edge.decoboon.customer.service.tagging.save;

import com.th4.edge.decoboon.customer.model.CustomerTagLink;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 给客户打人工标签。 */
public interface Save extends Provider<CustomerTagLink, CustomerTagLink>, Security {
}
