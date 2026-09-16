package com.th4.edge.tenant.user.service.user.designer.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

public interface List extends Provider<Employee, java.util.List<Employee>>, Security {
}
