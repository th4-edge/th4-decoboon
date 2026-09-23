package com.th4.edge.decoboon.project.service.contractchange.cancel;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 撤回待审批的合同变更单。 */
public interface Cancel extends ModelService<ContractChange, ContractChange>, Security {
}
