package com.th4.edge.platform.service.dashboard.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformMetrics;
import com.th4.edge.tenant.user.model.Company;

/** 平台运营总览。 */
public interface Dashboard extends Provider<Company, PlatformMetrics>, Security {
}
