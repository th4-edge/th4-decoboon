package com.th4.edge.decoboon.project.service.contractchange.info;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 合同变更单详情（含明细）。 */
public interface Info extends ModelService<ContractChange, ContractChange>, Security {
}
