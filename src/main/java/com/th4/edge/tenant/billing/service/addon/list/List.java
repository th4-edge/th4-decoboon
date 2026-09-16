package com.th4.edge.tenant.billing.service.addon.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 套餐加购商品列表。
 */
public interface List extends Provider<Employee, java.util.List<PlanAddon>>, Security {
}
