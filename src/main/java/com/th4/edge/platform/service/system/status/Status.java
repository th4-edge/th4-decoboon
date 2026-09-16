package com.th4.edge.platform.service.system.status;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.SystemStatus;

/** 平台服务器运行状态。 */
public interface Status extends Provider<PlatformUser, SystemStatus>, Security {
}
