package com.th4.edge.platform.service.tenant.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Company;

/** 平台企业详情。 */
public interface Info extends Provider<Company, Company>, Security {
}
