package com.th4.edge.platform.service.plan.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformPlan;
import com.th4.edge.platform.model.PlatformUser;

/** 平台可售套餐列表。 */
public interface List extends Provider<PlatformUser, java.util.List<PlatformPlan>>, Security {
}
