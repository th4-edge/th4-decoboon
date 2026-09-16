package com.th4.edge.decoboon.project.service.contractchange.save;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 发起合同变更单。 */
public interface Save extends ModelService<ContractChange, ContractChange>, Security {
}
