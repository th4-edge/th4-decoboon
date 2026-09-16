package com.th4.edge.tenant.billing.service.plan.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 可续费套餐列表。
 */
public interface List extends Provider<Employee, java.util.List<Plan>>, Security {
}
