package com.th4.edge.decoboon.customer.service.contract.workbench;

import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主合同履约档案只读聚合服务。 */
public interface Workbench extends Provider<ProjectContract, ContractWorkbench>, Security {
}
