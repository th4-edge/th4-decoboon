package com.th4.edge.decoboon.standardasset.service.lifecycle.list;

import com.th4.edge.decoboon.standardasset.model.StandardAssetLifecycleContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 企业运营标准资产生命周期只读契约。 */
public interface List extends Provider<Employee, java.util.List<StandardAssetLifecycleContract>>, Security {
}
