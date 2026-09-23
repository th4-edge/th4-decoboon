package com.th4.edge.decoboon.project.service.contractlines.page;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 合同基线行（签约冻结快照）分页服务。 */
public interface Page extends Provider<ContractLine, org.springframework.data.domain.Page<ContractLine>>, Security {
}
