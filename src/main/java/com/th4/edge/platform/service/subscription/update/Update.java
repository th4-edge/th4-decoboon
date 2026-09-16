package com.th4.edge.platform.service.subscription.update;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.user.model.Employee;

/** 平台人工调整订阅。 */
public interface Update extends Provider<Employee, Subscription>, Security {
}
