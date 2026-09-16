package com.th4.edge.platform.service.subscription.update;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformSubscription;
import com.th4.edge.platform.model.PlatformUser;

/** 平台人工调整订阅。 */
public interface Update extends Provider<PlatformUser, PlatformSubscription>, Security {
}
