package com.th4.edge.decoboon.customer.service.contractlines.page;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主合同范围只读分页服务。 */
public interface Page extends Provider<ContractLine, org.springframework.data.domain.Page<ContractLine>>, Security {
}
