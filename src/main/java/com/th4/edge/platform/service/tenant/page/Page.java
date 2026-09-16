package com.th4.edge.platform.service.tenant.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformTenant;
import com.th4.edge.platform.model.PlatformUser;

/** 平台企业分页。 */
public interface Page extends Provider<PlatformUser, org.springframework.data.domain.Page<PlatformTenant>>, Security {
}
