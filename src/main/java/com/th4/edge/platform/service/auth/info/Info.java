package com.th4.edge.platform.service.auth.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformUser;

/** 平台当前用户。 */
public interface Info extends Provider<PlatformUser, PlatformUser>, Security {
}
