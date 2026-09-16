package com.th4.edge.tenant.user.service.user.store.list;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;

/** 当前账号可见门店列表服务。 */
public interface List extends Provider<Employee, java.util.List<Store>>, Security {
}
