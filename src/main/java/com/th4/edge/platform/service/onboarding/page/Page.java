package com.th4.edge.platform.service.onboarding.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformTenantOnboarding;
import com.th4.edge.platform.model.PlatformUser;

/** 企业入驻台账分页。 */
public interface Page extends Provider<PlatformUser, org.springframework.data.domain.Page<PlatformTenantOnboarding>>, Security {
}
