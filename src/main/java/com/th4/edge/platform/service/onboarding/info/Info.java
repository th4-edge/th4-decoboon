package com.th4.edge.platform.service.onboarding.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformTenantOnboarding;
import com.th4.edge.platform.model.PlatformUser;

/** 企业入驻台账详情。 */
public interface Info extends Provider<PlatformUser, PlatformTenantOnboarding>, Security {
}
