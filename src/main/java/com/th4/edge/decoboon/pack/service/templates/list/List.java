package com.th4.edge.decoboon.pack.service.templates.list;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 可用于新报价的当前发布套餐。 */
public interface List extends Provider<Employee, java.util.List<PackageTemplate>>, Security {
}
