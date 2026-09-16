package com.th4.edge.platform.service.auth.logout;

import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformUser;

/** 平台独立登出。 */
public interface Logout extends com.th4.edge.framework.provider.security.logout.Logout<PlatformUser>, Security {
}
