package com.th4.edge.platform.service.tenant.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Company;

/** 平台企业分页。 */
public interface Page extends Provider<Company, org.springframework.data.domain.Page<Company>>, Security {
}
