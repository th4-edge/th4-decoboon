package com.th4.edge.platform.service.onboarding.reveal;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformTenantOwnerContact;
import com.th4.edge.platform.model.PlatformUser;

/** 揭示企业负责人手机号。 */
public interface Reveal extends Provider<PlatformUser, PlatformTenantOwnerContact>, Security {
}
