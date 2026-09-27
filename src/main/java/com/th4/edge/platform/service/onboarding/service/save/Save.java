package com.th4.edge.platform.service.onboarding.service.save;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformOnboardingServiceView;
import com.th4.edge.platform.model.PlatformUser;

/** 保存企业入驻服务记录。 */
public interface Save extends Provider<PlatformUser, PlatformOnboardingServiceView>, Security {
}
