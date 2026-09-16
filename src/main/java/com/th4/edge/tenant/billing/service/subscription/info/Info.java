package com.th4.edge.tenant.billing.service.subscription.info;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 当前企业订阅与额度信息。
 */
public interface Info extends Provider<Employee, Subscription>, Security {
}
