package com.th4.edge.decoboon.customer.service.contract.info;

import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主授权项目合同摘要。 */
public interface Info extends Provider<ProjectContract, ProjectContract>, Security {
}
