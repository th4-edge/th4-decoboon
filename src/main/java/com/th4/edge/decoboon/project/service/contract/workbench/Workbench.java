package com.th4.edge.decoboon.project.service.contract.workbench;

import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 合同履约档案聚合服务。 */
public interface Workbench extends Provider<ProjectContract, ContractWorkbench>, Security {
}
