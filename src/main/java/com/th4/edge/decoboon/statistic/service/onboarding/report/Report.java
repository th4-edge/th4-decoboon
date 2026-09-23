package com.th4.edge.decoboon.statistic.service.onboarding.report;

import com.th4.edge.decoboon.statistic.model.OnboardingGuide;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Credentials;

/** 当前角色启动引导状态投影服务。 */
public interface Report extends Provider<Credentials, OnboardingGuide>, Security {
}
